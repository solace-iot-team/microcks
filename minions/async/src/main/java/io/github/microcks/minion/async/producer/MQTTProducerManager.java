/*
 * Licensed to Laurent Broudoux (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.microcks.minion.async.producer;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.jboss.logging.Logger;

import io.github.microcks.domain.EventMessage;
import io.github.microcks.minion.async.AsyncMockDefinition;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.UnsupportedEncodingException;

/**
 * MQTT implementation of producer for async event messages.
 * 
 * @author laurent
 */
@ApplicationScoped
public class MQTTProducerManager extends BindingProducerManager {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private IMqttClient client;

   @ConfigProperty(name = "mqtt.server")
   String mqttServer;

   @ConfigProperty(name = "mqtt.clientid", defaultValue = "microcks-async-minion")
   String mqttClientId;

   @ConfigProperty(name = "mqtt.username")
   String mqttUsername;

   @ConfigProperty(name = "mqtt.password")
   String mqttPassword;

   /**
    * Initialize the MQTT client post construction.
    * @throws Exception If connection to MQTT Broker cannot be done.
    */
   @PostConstruct
   public void create() throws Exception {
      try {
         client = createClient();
      } catch (Exception e) {
         logger.errorf("Cannot connect to MQTT broker %s", mqttServer);
         throw e;
      }
   }

   /**
    * Create a IMqttClient and connect it to the server.
    * 
    * @return A new IMqttClient implementation initialized with configuration properties.
    * @throws Exception in case of connection failure
    */
   protected IMqttClient createClient() throws Exception {
      MqttConnectOptions options = new MqttConnectOptions();
      if (mqttUsername != null && mqttUsername.length() > 0 && mqttPassword != null && mqttPassword.length() > 0) {
         options.setUserName(mqttUsername);
         options.setPassword(mqttPassword.toCharArray());
      }
      options.setAutomaticReconnect(true);
      // Set clean session to false as we're reusing the same clientId.
      options.setCleanSession(false);
      options.setConnectionTimeout(20);
      options.setKeepAliveInterval(10);
      IMqttClient publisher = new MqttClient("tcp://" + mqttServer, mqttClientId);
      publisher.connect(options);
      return publisher;
   }

   /**
    * Publish a message on specified topic.
    * 
    * @param topic The destination topic for message
    * @param value The message payload
    */
   private void publishMessage(String topic, String value) {
      logger.infof("Publishing on topic {%s}, message: %s ", topic, value);
      try {
         client.publish(topic, value.getBytes("UTF-8"), 0, false);
      } catch (UnsupportedEncodingException uee) {
         logger.warnf("Message %s cannot be encoded as UTF-8 bytes, ignoring it", uee);
      } catch (MqttPersistenceException mpe) {
         mpe.printStackTrace();
      } catch (MqttException me) {
         me.printStackTrace();
      }
   }

   @Override
   protected void publishOne(AsyncMockDefinition definition, EventMessage eventMessage, String topic) {
      String message = renderEventMessageContent(eventMessage);
      this.publishMessage(topic, message);
   }

}
