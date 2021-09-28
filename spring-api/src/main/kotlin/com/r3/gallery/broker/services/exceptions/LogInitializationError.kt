package com.r3.gallery.broker.services.exceptions

// TODO Add additional exceptions
class LogInitializationError(issue: String) :
    IllegalStateException("Unable to initialize LogService. " + issue)