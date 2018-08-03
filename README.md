Logging
===

To make Vert.x use SLF4J, set following system property

    vertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory

To specify the logback configuration, use this property

    logback.configurationFile=<classpath|filepath>
    
    
Environment variables
===

* `HTTP_PORT`: Port to listen on