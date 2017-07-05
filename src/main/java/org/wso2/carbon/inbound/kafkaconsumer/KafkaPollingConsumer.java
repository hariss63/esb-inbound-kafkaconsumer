/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.inbound.kafkaconsumer;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.wso2.carbon.inbound.endpoint.protocol.generic.GenericPollingConsumer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Kafka Polling Consumer.
 *
 * @since 1.0.0.
 */
public class KafkaPollingConsumer extends GenericPollingConsumer {

    private static final Log log = LogFactory.getLog(KafkaPollingConsumer.class);

    private boolean isPolled = false;
    private KafkaConsumer<byte[], byte[]> consumer;
    private MessageContext msgCtx;

    public KafkaPollingConsumer(Properties properties, String name, SynapseEnvironment synapseEnvironment,
            long scanInterval, String injectingSeq, String onErrorSeq, boolean coordination, boolean sequential) {

        super(properties, name, synapseEnvironment, scanInterval, injectingSeq, onErrorSeq, coordination, sequential);

        loadMandatoryParameters(properties);
        loadOptionalParameters(properties);
    }

    public void connect() {

        String[] topicsArray = (properties.getProperty(KafkaConstants.TOPIC_NAME)).split(",");
        List<String> topics = Arrays.asList(topicsArray);
        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(topics);

        try {
            while (true) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                for (ConsumerRecord record : records) {
                    msgCtx = createMessageContext();
                    msgCtx.setProperty("partitionNo", record.partition());
                    msgCtx.setProperty("messageValue", record.value());
                    msgCtx.setProperty("offset", record.offset());
                    injectMessage(record.value().toString(), properties.getProperty(KafkaConstants.CONTENT_TYPE));
                }

                if (!records.isEmpty()) {
                    consumer.commitAsync();
                }
            }
        } catch (WakeupException e) {
            throw new SynapseException("Error while wakeup the Kafka Consumer", e);
        }
    }

    @Override protected boolean injectMessage(String strMessage, String contentType) {
        AutoCloseInputStream in = new AutoCloseInputStream(new ByteArrayInputStream(strMessage.getBytes()));
        return this.injectMessage((InputStream) in, contentType);
    }

    @Override protected boolean injectMessage(InputStream in, String contentType) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Processed Custom inbound EP Message of Content-type : " + contentType + " for " + name);
            }

            org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) msgCtx)
                    .getAxis2MessageContext();
            Object builder;
            if (StringUtils.isEmpty(contentType)) {
                log.warn("Unable to determine content type for message, setting to application/json for " + name);
            }
            int index = contentType.indexOf(';');
            String type = index > 0 ? contentType.substring(0, index) : contentType;
            builder = BuilderUtil.getBuilderFromSelector(type, axis2MsgCtx);
            if (builder == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No message builder found for type '" + type + "'. Falling back to SOAP. for" + name);
                }
                builder = new SOAPBuilder();
            }

            OMElement documentElement1 = ((Builder) builder).processDocument(in, contentType, axis2MsgCtx);
            msgCtx.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement1));
            if (this.injectingSeq == null || "".equals(this.injectingSeq)) {
                log.error("Sequence name not specified. Sequence : " + this.injectingSeq + " for " + name);
                return false;
            }

            SequenceMediator seq = (SequenceMediator) this.synapseEnvironment.getSynapseConfiguration()
                    .getSequence(this.injectingSeq);
            seq.setErrorHandler(this.onErrorSeq);
            if (seq != null) {
                if (log.isDebugEnabled()) {
                    log.debug("injecting message to sequence : " + this.injectingSeq + " of " + name);
                }
                if (!this.synapseEnvironment.injectInbound(msgCtx, seq, this.sequential)) {
                    return false;
                }
            } else {
                log.error("Sequence: " + this.injectingSeq + " not found for " + name);
            }
        } catch (Exception e) {
            log.error("Error while processing the Kafka inbound endpoint Message ", e);
        }
        return true;
    }

    /**
     * load essential property for Kafka inbound endpoint.
     *
     * @param properties The mandatory parameters of Kafka.
     */
    private void loadMandatoryParameters(Properties properties) {
        if (log.isDebugEnabled()) {
            log.debug("Starting to load the Kafka Mandatory parameters");
        }

        String bootstrapServersName = properties.getProperty(KafkaConstants.BOOTSTRAP_SERVERS_NAME);
        String keyDeserializer = properties.getProperty(KafkaConstants.KEY_DESERIALIZER);
        String valueDeserializer = properties.getProperty(KafkaConstants.VALUE_DESERIALIZER);
        String groupId = properties.getProperty(KafkaConstants.GROUP_ID);

        if (StringUtils.isEmpty(bootstrapServersName) || StringUtils.isEmpty(keyDeserializer) || StringUtils
                .isEmpty(valueDeserializer) || StringUtils.isEmpty(groupId)) {
            throw new SynapseException("Mandatory Parameters can't be Empty...");
        }
    }

    /**
     * Load optional parameters for Kafka inbound endpoint.
     *
     * @param properties The Optional parameters of Kafka.
     */
    private void loadOptionalParameters(Properties properties) {
        if (log.isDebugEnabled()) {
            log.debug("Starting to load the Kafka optional parameters");
        }

        if (properties.getProperty(KafkaConstants.ENABLE_AUTO_COMMIT) == null) {
            properties.setProperty(KafkaConstants.ENABLE_AUTO_COMMIT, KafkaConstants.ENABLE_AUTO_COMMIT_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.ENABLE_AUTO_COMMIT);
        }

        if (properties.getProperty(KafkaConstants.AUTO_COMMIT_INTERVAL_MS) == null) {
            properties.setProperty(KafkaConstants.AUTO_COMMIT_INTERVAL_MS,
                    KafkaConstants.AUTO_COMMIT_INTERVAL_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.AUTO_COMMIT_INTERVAL_MS);
        }

        if (properties.getProperty(KafkaConstants.SESSION_TIMEOUT_MS) == null) {
            properties.setProperty(KafkaConstants.SESSION_TIMEOUT_MS, KafkaConstants.SESSION_TIMEOUT_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SESSION_TIMEOUT_MS);
        }

        if (properties.getProperty(KafkaConstants.FETCH_MIN_BYTES) == null) {
            properties.setProperty(KafkaConstants.FETCH_MIN_BYTES, KafkaConstants.FETCH_MIN_BYTES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.FETCH_MIN_BYTES);
        }

        if (properties.getProperty(KafkaConstants.HEARTBEAT_INTERVAL_MS) == null) {
            properties.setProperty(KafkaConstants.HEARTBEAT_INTERVAL_MS, KafkaConstants.HEARTBEAT_INTERVAL_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.HEARTBEAT_INTERVAL_MS);
        }

        if (properties.getProperty(KafkaConstants.MAX_PARTITION_FETCH_BYTES) == null) {
            properties.setProperty(KafkaConstants.MAX_PARTITION_FETCH_BYTES,
                    KafkaConstants.MAX_PARTITION_FETCH_BYTES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.MAX_PARTITION_FETCH_BYTES);
        }

        if (properties.getProperty(KafkaConstants.SSL_KEY_PASSWORD) == null) {
            properties.setProperty(KafkaConstants.SSL_KEY_PASSWORD, KafkaConstants.SSL_KEY_PASSWORD_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_KEY_PASSWORD);
        }

        if (properties.getProperty(KafkaConstants.SSL_KEYSTORE_LOCATION) == null) {
            properties.setProperty(KafkaConstants.SSL_KEYSTORE_LOCATION, KafkaConstants.SSL_KEYSTORE_LOCATION_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_KEYSTORE_LOCATION);
        }

        if (properties.getProperty(KafkaConstants.SSL_KEYSTORE_PASSWORD) == null) {
            properties.setProperty(KafkaConstants.SSL_KEYSTORE_PASSWORD, KafkaConstants.SSL_KEYSTORE_PASSWORD_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_KEYSTORE_PASSWORD);
        }

        if (properties.getProperty(KafkaConstants.SSL_TRUSTORE_LOCATION) == null) {
            properties.setProperty(KafkaConstants.SSL_TRUSTORE_LOCATION, KafkaConstants.SSL_TRUSTORE_LOCATION_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_TRUSTORE_LOCATION);
        }

        if (properties.getProperty(KafkaConstants.SSL_TRUSTORE_PASSWORD) == null) {
            properties.setProperty(KafkaConstants.SSL_TRUSTORE_PASSWORD, KafkaConstants.SSL_TRUSTORE_PASSWORD_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_TRUSTORE_PASSWORD);
        }

        if (properties.getProperty(KafkaConstants.AUTO_OFFSET_RESET) == null) {
            properties.setProperty(KafkaConstants.AUTO_OFFSET_RESET, KafkaConstants.AUTO_OFFSET_RESET_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.AUTO_OFFSET_RESET);
        }

        if (properties.getProperty(KafkaConstants.CONNECTIONS_MAX_IDLE_MS) == null) {
            properties.setProperty(KafkaConstants.CONNECTIONS_MAX_IDLE_MS,
                    KafkaConstants.CONNECTIONS_MAX_IDLE_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.CONNECTIONS_MAX_IDLE_MS);
        }

        if (properties.getProperty(KafkaConstants.EXCLUDE_INTERNAL_TOPICS) == null) {
            properties.setProperty(KafkaConstants.EXCLUDE_INTERNAL_TOPICS,
                    KafkaConstants.EXCLUDE_INTERNAL_TOPICS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.EXCLUDE_INTERNAL_TOPICS);
        }

        if (properties.getProperty(KafkaConstants.FETCH_MAX_BYTES) == null) {
            properties.setProperty(KafkaConstants.FETCH_MAX_BYTES, KafkaConstants.FETCH_MAX_BYTES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.FETCH_MAX_BYTES);
        }

        if (properties.getProperty(KafkaConstants.MAX_POLL_INTERVAL_MS) == null) {
            properties.setProperty(KafkaConstants.MAX_POLL_INTERVAL_MS, KafkaConstants.MAX_POLL_INTERVAL_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.MAX_POLL_INTERVAL_MS);
        }

        if (properties.getProperty(KafkaConstants.MAX_POLL_RECORDS) == null) {
            properties.setProperty(KafkaConstants.MAX_POLL_RECORDS, KafkaConstants.MAX_POLL_RECORDS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.MAX_POLL_RECORDS);
        }

        if (properties.getProperty(KafkaConstants.PARTITION_ASSIGNMENT_STRATEGY) == null) {
            properties.setProperty(KafkaConstants.PARTITION_ASSIGNMENT_STRATEGY,
                    KafkaConstants.PARTITION_ASSIGNMENT_STRATEGY_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.PARTITION_ASSIGNMENT_STRATEGY);
        }

        if (properties.getProperty(KafkaConstants.RECEIVER_BUFFER_BYTES) == null) {
            properties.setProperty(KafkaConstants.RECEIVER_BUFFER_BYTES, KafkaConstants.RECEIVER_BUFFER_BYTES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.RECEIVER_BUFFER_BYTES);
        }

        if (properties.getProperty(KafkaConstants.REQUEST_TIMEOUT_MS) == null) {
            properties.setProperty(KafkaConstants.REQUEST_TIMEOUT_MS, KafkaConstants.REQUEST_TIMEOUT_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.REQUEST_TIMEOUT_MS);
        }

        if (properties.getProperty(KafkaConstants.SASL_JAAS_CONFIG) == null) {
            properties.setProperty(KafkaConstants.SASL_JAAS_CONFIG, KafkaConstants.SASL_JAAS_CONFIG_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_JAAS_CONFIG);
        }

        if (properties.getProperty(KafkaConstants.SASL_KERBEROS_SERVICE_NAME) == null) {
            properties.setProperty(KafkaConstants.SASL_KERBEROS_SERVICE_NAME,
                    KafkaConstants.SASL_KERBEROS_SERVICE_NAME_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_KERBEROS_SERVICE_NAME);
        }

        if (properties.getProperty(KafkaConstants.SASL_MECANISM) == null) {
            properties.setProperty(KafkaConstants.SASL_MECANISM, KafkaConstants.SASL_MECANISM_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_MECANISM);
        }

        if (properties.getProperty(KafkaConstants.SECURITY_PROTOCOL) == null) {
            properties.setProperty(KafkaConstants.SECURITY_PROTOCOL, KafkaConstants.SECURITY_PROTOCOL_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SECURITY_PROTOCOL);
        }

        if (properties.getProperty(KafkaConstants.SEND_BUFFER_BYTES) == null) {
            properties.setProperty(KafkaConstants.SEND_BUFFER_BYTES, KafkaConstants.SEND_BUFFER_BYTES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SEND_BUFFER_BYTES);
        }

        if (properties.getProperty(KafkaConstants.SSL_ENABLED_PROTOCOL) == null) {
            properties.setProperty(KafkaConstants.SSL_ENABLED_PROTOCOL, KafkaConstants.SSL_ENABLED_PROTOCOL_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_ENABLED_PROTOCOL);
        }

        if (properties.getProperty(KafkaConstants.SSL_KEYSTORE_TYPE) == null) {
            properties.setProperty(KafkaConstants.SSL_KEYSTORE_TYPE, KafkaConstants.SSL_KEYSTORE_TYPE_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_KEYSTORE_TYPE);
        }

        if (properties.getProperty(KafkaConstants.SSL_PROTOCOL) == null) {
            properties.setProperty(KafkaConstants.SSL_PROTOCOL, KafkaConstants.SSL_PROTOCOL_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_PROTOCOL);
        }

        if (properties.getProperty(KafkaConstants.SSL_PROVIDER) == null) {
            properties.setProperty(KafkaConstants.SSL_PROVIDER, KafkaConstants.SSL_PROVIDER_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_PROVIDER);
        }

        if (properties.getProperty(KafkaConstants.SSL_TRUSTSTORE_TYPE) == null) {
            properties.setProperty(KafkaConstants.SSL_TRUSTSTORE_TYPE, KafkaConstants.SSL_TRUSTSTORE_TYPE_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_TRUSTSTORE_TYPE);
        }

        if (properties.getProperty(KafkaConstants.CHECK_CRCS) == null) {
            properties.setProperty(KafkaConstants.CHECK_CRCS, KafkaConstants.CHECK_CRCS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.CHECK_CRCS);
        }

        if (properties.getProperty(KafkaConstants.CLIENT_ID) == null) {
            properties.setProperty(KafkaConstants.CLIENT_ID, KafkaConstants.CLIENT_ID_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.CLIENT_ID);
        }

        if (properties.getProperty(KafkaConstants.FETCH_MAX_WAIT_MS) == null) {
            properties.setProperty(KafkaConstants.FETCH_MAX_WAIT_MS, KafkaConstants.FETCH_MAX_WAIT_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.FETCH_MAX_WAIT_MS);
        }

        if (properties.getProperty(KafkaConstants.INTERCEPTOR_CLASSES) == null) {
            properties.setProperty(KafkaConstants.INTERCEPTOR_CLASSES, KafkaConstants.INTERCEPTOR_CLASSES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.INTERCEPTOR_CLASSES);
        }

        if (properties.getProperty(KafkaConstants.METADATA_MAX_AGE_MS) == null) {
            properties.setProperty(KafkaConstants.METADATA_MAX_AGE_MS, KafkaConstants.METADATA_MAX_AGE_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.METADATA_MAX_AGE_MS);
        }

        if (properties.getProperty(KafkaConstants.METRIC_REPORTERS) == null) {
            properties.setProperty(KafkaConstants.METRIC_REPORTERS, KafkaConstants.METRIC_REPORTERS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.METRIC_REPORTERS);
        }

        if (properties.getProperty(KafkaConstants.METRICS_NUM_SAMPLES) == null) {
            properties.setProperty(KafkaConstants.METRICS_NUM_SAMPLES, KafkaConstants.METRICS_NUM_SAMPLES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.METRICS_NUM_SAMPLES);
        }

        if (properties.getProperty(KafkaConstants.METRICS_RECORDING_LEVEL) == null) {
            properties.setProperty(KafkaConstants.METRICS_RECORDING_LEVEL,
                    KafkaConstants.METRICS_RECORDING_LEVEL_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.METRICS_RECORDING_LEVEL);
        }

        if (properties.getProperty(KafkaConstants.METRICS_SAMPLE_WINDOW_MS) == null) {
            properties.setProperty(KafkaConstants.METRICS_SAMPLE_WINDOW_MS,
                    KafkaConstants.METRICS_SAMPLE_WINDOW_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.METRICS_SAMPLE_WINDOW_MS);
        }

        if (properties.getProperty(KafkaConstants.RECONNECT_BACKOFF_MS) == null) {
            properties.setProperty(KafkaConstants.RECONNECT_BACKOFF_MS, KafkaConstants.RECONNECT_BACKOFF_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.RECONNECT_BACKOFF_MS);
        }

        if (properties.getProperty(KafkaConstants.RETRY_BACKOFF_MS) == null) {
            properties.setProperty(KafkaConstants.RETRY_BACKOFF_MS, KafkaConstants.RETRY_BACKOFF_MS_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.RETRY_BACKOFF_MS);
        }

        if (properties.getProperty(KafkaConstants.SASL_KERBEROS_KINIT_CMD) == null) {
            properties.setProperty(KafkaConstants.SASL_KERBEROS_KINIT_CMD,
                    KafkaConstants.SASL_KERBEROS_KINIT_CMD_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_KERBEROS_KINIT_CMD);
        }

        if (properties.getProperty(KafkaConstants.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN) == null) {
            properties.setProperty(KafkaConstants.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN,
                    KafkaConstants.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN);
        }

        if (properties.getProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_JITTER) == null) {
            properties.setProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_JITTER,
                    KafkaConstants.SASL_KERBEROS_TICKET_RENEW_JITTER_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_JITTER);
        }

        if (properties.getProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR) == null) {
            properties.setProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR,
                    KafkaConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR);
        }

        if (properties.getProperty(KafkaConstants.SSL_CIPHER_SUITES) == null) {
            properties.setProperty(KafkaConstants.SSL_CIPHER_SUITES, KafkaConstants.SSL_CIPHER_SUITES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_CIPHER_SUITES);
        }

        if (properties.getProperty(KafkaConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM) == null) {
            properties.setProperty(KafkaConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM,
                    KafkaConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM);
        }

        if (properties.getProperty(KafkaConstants.SSL_CIPHER_SUITES) == null) {
            properties.setProperty(KafkaConstants.SSL_CIPHER_SUITES, KafkaConstants.SSL_CIPHER_SUITES_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_CIPHER_SUITES);
        }

        if (properties.getProperty(KafkaConstants.SSL_KEYMANAGER_ALGORITHM) == null) {
            properties.setProperty(KafkaConstants.SSL_KEYMANAGER_ALGORITHM,
                    KafkaConstants.SSL_KEYMANAGER_ALGORITHM_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_KEYMANAGER_ALGORITHM);
        }

        if (properties.getProperty(KafkaConstants.SSL_SECURE_RANDOM_IMPLEMENTATION) == null) {
            properties.setProperty(KafkaConstants.SSL_SECURE_RANDOM_IMPLEMENTATION,
                    KafkaConstants.SSL_SECURE_RANDOM_IMPLEMENTATION_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_SECURE_RANDOM_IMPLEMENTATION);
        }

        if (properties.getProperty(KafkaConstants.SSL_TRUSTMANAGER_ALGORITHM) == null) {
            properties.setProperty(KafkaConstants.SSL_TRUSTMANAGER_ALGORITHM,
                    KafkaConstants.SSL_TRUSTMANAGER_ALGORITHM_DEFAULT);
        } else {
            properties.getProperty(KafkaConstants.SSL_TRUSTMANAGER_ALGORITHM);
        }
    }

    /**
     * Create the message context.
     */
    private MessageContext createMessageContext() {
        MessageContext msgCtx = this.synapseEnvironment.createMessageContext();
        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) msgCtx).getAxis2MessageContext();
        axis2MsgCtx.setServerSide(true);
        axis2MsgCtx.setMessageID(String.valueOf(UUID.randomUUID()));
        return msgCtx;
    }

    @Override public Object poll() {
        if (!isPolled) {
            connect();
            isPolled = true;
        }
        return null;
    }

    /**
     * Close the connection to the Kafka.
     */
    public void destroy() {
        try {
            if (consumer != null) {
                consumer.close();
                if (log.isDebugEnabled()) {
                    log.debug("The Kafka consumer has been close ! for " + name);
                }
            }
        } catch (Exception e) {
            log.error("Error while shutdown the Kafka consumer " + name + " " + e.getMessage(), e);
        }
    }
}