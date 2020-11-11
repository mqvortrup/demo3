# Backoff und Jitter

Grundimplementation mit SpringBoot JPA/Hibernate.

In src/main/resources/application.properties müssen url, username und password für eine existirende DB gesetzt werden.

Zwei Implementationen von "Exponential Backoff w/jitter" - eine manuelle, und eine mit dem Library [Resilience4J](https://resilience4j.readme.io/docs/getting-started)

Beide Implementation sind in der Klasse com.example.demo.BackoffAndJitter.

#### Manuelle Implementation

Zum Aktivieren, @Bean in Zeile 37 (methode demoManual()) 'einkommentieren', und in Zeile 79 (methode demoUtility()) auskommentieren.

Alle Schritte sind direkt implementiert.

#### Resilience4J Implementation

Zum Aktivieren, @Bean in Zeile 37 (methode demoManual()) 'auskommentieren', und in Zeile 79 (methode demoUtility()) 'einkommentieren'.

Exponential Backoff und Jitter sind mit IntervalFunction, RetryConfig und Retry aus Resilience4J.