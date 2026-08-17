open module fantasy.storefront {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;

    exports app;
    exports model;
    exports service;
}
