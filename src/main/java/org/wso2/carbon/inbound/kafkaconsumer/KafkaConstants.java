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

/**
 * Kafka Constants.
 */
public class KafkaConstants {

    //Mandatory parameter for Kafka Inbound Endpoint.
    public static final String TOPIC_NAME = "topic";
    public static final String CONTENT_TYPE = "contentType";
    public static final String BOOTSTRAP_SERVERS_NAME = "bootstrap.servers";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";
    public static final String GROUP_ID = "group.id";

    //Optional parameters for Kafka Inbound Endpoint.
    public static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";
    public static final String AUTO_COMMIT_INTERVAL_MS = "auto.commit.interval.ms";
    public static final String SESSION_TIMEOUT_MS = "session.timeout.ms";
    public static final String FETCH_MIN_BYTES = "fetch.min.bytes";
    public static final String HEARTBEAT_INTERVAL_MS = "heartbeat.interval.ms";
    public static final String MAX_PARTITION_FETCH_BYTES = "max.partition.fetch.bytes";
    public static final String SSL_KEY_PASSWORD = "ssl.key.password";
    public static final String SSL_KEYSTORE_LOCATION = "ssl.keystore.location";
    public static final String SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";
    public static final String SSL_TRUSTORE_LOCATION = "ssl.truststore.location";
    public static final String SSL_TRUSTORE_PASSWORD = "ssl.truststore.password";
    public static final String AUTO_OFFSET_RESET = "auto.offset.reset";
    public static final String CONNECTIONS_MAX_IDLE_MS = "connections.max.idle.ms";
    public static final String EXCLUDE_INTERNAL_TOPICS = "exclude.internal.topics";
    public static final String FETCH_MAX_BYTES = "fetch.max.bytes";
    public static final String MAX_POLL_INTERVAL_MS = "max.poll.interval.ms";
    public static final String MAX_POLL_RECORDS = "max.poll.records";
    public static final String PARTITION_ASSIGNMENT_STRATEGY = "partition.assignment.strategy";
    public static final String RECEIVER_BUFFER_BYTES = "receive.buffer.bytes";
    public static final String REQUEST_TIMEOUT_MS = "request.timeout.ms";
    public static final String SASL_JAAS_CONFIG = "sasl.jaas.config";
    public static final String SASL_KERBEROS_SERVICE_NAME = "sasl.kerberos.service.name";
    public static final String SASL_MECANISM = "sasl.mechanism";
    public static final String SECURITY_PROTOCOL = "security.protocol";
    public static final String SEND_BUFFER_BYTES = "send.buffer.bytes";
    public static final String SSL_ENABLED_PROTOCOL = "ssl.enabled.protocols";
    public static final String SSL_KEYSTORE_TYPE = "ssl.keystore.type";
    public static final String SSL_PROTOCOL = "ssl.protocol";
    public static final String SSL_PROVIDER = "ssl.provider";
    public static final String SSL_TRUSTSTORE_TYPE = "ssl.truststore.type";
    public static final String CHECK_CRCS = "check.crcs";
    public static final String CLIENT_ID = "client.id";
    public static final String FETCH_MAX_WAIT_MS = "fetch.max.wait.ms";
    public static final String INTERCEPTOR_CLASSES = "interceptor.classes";
    public static final String METADATA_MAX_AGE_MS = "metadata.max.age.ms";
    public static final String METRIC_REPORTERS = "metric.reporters";
    public static final String METRICS_NUM_SAMPLES = "metrics.num.samples";
    public static final String METRICS_RECORDING_LEVEL = "metrics.recording.level";
    public static final String METRICS_SAMPLE_WINDOW_MS = "metrics.sample.window.ms";
    public static final String RECONNECT_BACKOFF_MS = "reconnect.backoff.ms";
    public static final String RETRY_BACKOFF_MS = "retry.backoff.ms";
    public static final String SASL_KERBEROS_KINIT_CMD = "sasl.kerberos.kinit.cmd";
    public static final String SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN = "sasl.kerberos.min.time.before.relogin";
    public static final String SASL_KERBEROS_TICKET_RENEW_JITTER = "sasl.kerberos.ticket.renew.jitter";
    public static final String SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR = "sasl.kerberos.ticket.renew.window.factor";
    public static final String SSL_CIPHER_SUITES = "ssl.cipher.suites";
    public static final String SSL_ENDPOINT_IDENTIFICATION_ALGORITHM = "ssl.endpoint.identification.algorithm";
    public static final String SSL_KEYMANAGER_ALGORITHM = "ssl.keymanager.algorithm";
    public static final String SSL_SECURE_RANDOM_IMPLEMENTATION = "ssl.secure.random.implementation";
    public static final String SSL_TRUSTMANAGER_ALGORITHM = "ssl.trustmanager.algorithm";

    //Kafka Inbound endpoint parameter's default value.
    public static final String ENABLE_AUTO_COMMIT_DEFAULT = "true";
    public static final String AUTO_COMMIT_INTERVAL_MS_DEFAULT = "5000";
    public static final String SESSION_TIMEOUT_MS_DEFAULT = "10000";
    public static final String FETCH_MIN_BYTES_DEFAULT = "1";
    public static final String HEARTBEAT_INTERVAL_MS_DEFAULT = "3000";
    public static final String MAX_PARTITION_FETCH_BYTES_DEFAULT = "1048576";
    public static final String SSL_KEY_PASSWORD_DEFAULT = "";
    public static final String SSL_KEYSTORE_LOCATION_DEFAULT = "";
    public static final String SSL_KEYSTORE_PASSWORD_DEFAULT = "";
    public static final String SSL_TRUSTORE_LOCATION_DEFAULT = "";
    public static final String SSL_TRUSTORE_PASSWORD_DEFAULT = "";
    public static final String AUTO_OFFSET_RESET_DEFAULT = "latest";
    public static final String CONNECTIONS_MAX_IDLE_MS_DEFAULT = "540000";
    public static final String EXCLUDE_INTERNAL_TOPICS_DEFAULT = "true";
    public static final String FETCH_MAX_BYTES_DEFAULT = "52428800";
    public static final String MAX_POLL_INTERVAL_MS_DEFAULT = "300000";
    public static final String MAX_POLL_RECORDS_DEFAULT = "500";
    public static final String PARTITION_ASSIGNMENT_STRATEGY_DEFAULT = "org.apache.kafka.clients.consumer.RangeAssignor";
    public static final String RECEIVER_BUFFER_BYTES_DEFAULT = "65536";
    public static final String REQUEST_TIMEOUT_MS_DEFAULT = "305000";
    public static final String SASL_JAAS_CONFIG_DEFAULT = "";
    public static final String SASL_KERBEROS_SERVICE_NAME_DEFAULT = "";
    public static final String SASL_MECANISM_DEFAULT = "GSSAPI";
    public static final String SECURITY_PROTOCOL_DEFAULT = "PLAINTEXT";
    public static final String SEND_BUFFER_BYTES_DEFAULT = "131072";
    public static final String SSL_ENABLED_PROTOCOL_DEFAULT = "TLSv1.2";
    public static final String SSL_KEYSTORE_TYPE_DEFAULT = "JKS";
    public static final String SSL_PROTOCOL_DEFAULT = "TLS";
    public static final String SSL_PROVIDER_DEFAULT = "";
    public static final String SSL_TRUSTSTORE_TYPE_DEFAULT = "JKS";
    public static final String CHECK_CRCS_DEFAULT = "true";
    public static final String CLIENT_ID_DEFAULT = "";
    public static final String FETCH_MAX_WAIT_MS_DEFAULT = "500";
    public static final String INTERCEPTOR_CLASSES_DEFAULT = "";
    public static final String METADATA_MAX_AGE_MS_DEFAULT = "300000";
    public static final String METRIC_REPORTERS_DEFAULT = "";
    public static final String METRICS_NUM_SAMPLES_DEFAULT = "2";
    public static final String METRICS_RECORDING_LEVEL_DEFAULT = "INFO";
    public static final String METRICS_SAMPLE_WINDOW_MS_DEFAULT = "30000";
    public static final String RECONNECT_BACKOFF_MS_DEFAULT = "50";
    public static final String RETRY_BACKOFF_MS_DEFAULT = "100";
    public static final String SASL_KERBEROS_KINIT_CMD_DEFAULT = "/usr/bin/kinit";
    public static final String SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN_DEFAULT = "60000";
    public static final String SASL_KERBEROS_TICKET_RENEW_JITTER_DEFAULT = "0.05";
    public static final String SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR_DEFAULT = "0.8";
    public static final String SSL_CIPHER_SUITES_DEFAULT = "";
    public static final String SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_DEFAULT = "";
    public static final String SSL_KEYMANAGER_ALGORITHM_DEFAULT = "SunX509";
    public static final String SSL_SECURE_RANDOM_IMPLEMENTATION_DEFAULT = "";
    public static final String SSL_TRUSTMANAGER_ALGORITHM_DEFAULT = "PKIX";
}
