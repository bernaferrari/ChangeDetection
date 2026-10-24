package com.bernaferrari.changedetection.error

internal actual fun isPlatformTlsFailure(error: Throwable): Boolean = error is javax.net.ssl.SSLException || error is java.security.cert.CertificateException || error is java.security.cert.CertPathValidatorException || error is java.security.cert.CertPathBuilderException
