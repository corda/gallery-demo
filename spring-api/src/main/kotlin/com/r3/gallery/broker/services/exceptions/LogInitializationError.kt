package com.r3.gallery.broker.services.exceptions

class LogInitializationError(issue: String) :
    IllegalStateException("Unable to initialize LogService. " + issue)