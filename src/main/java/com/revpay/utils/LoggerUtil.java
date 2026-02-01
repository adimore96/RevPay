package com.revpay.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {

    // Private constructor to prevent instantiation
    private LoggerUtil() {}

    /**
     * Get logger for the calling class
     * @return Logger instance
     */
    public static Logger getLogger() {
        // Get the caller class name
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callerClassName = stackTrace[2].getClassName();
        return LogManager.getLogger(callerClassName);
    }

    /**
     * Get logger for specific class
     * @param clazz Class to get logger for
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    /**
     * Get logger by name
     * @param name Logger name
     * @return Logger instance
     */
    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

    /**
     * Log method entry
     * @param logger Logger instance
     * @param methodName Method name
     * @param params Method parameters
     */
    public static void logMethodEntry(Logger logger, String methodName, Object... params) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Entering ").append(methodName);
            if (params.length > 0) {
                sb.append(" with parameters: ");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(maskSensitiveData(params[i]));
                }
            }
            logger.debug(sb.toString());
        }
    }

    /**
     * Log method exit
     * @param logger Logger instance
     * @param methodName Method name
     * @param result Method return value (can be null)
     */
    public static void logMethodExit(Logger logger, String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            String message = "Exiting " + methodName;
            if (result != null) {
                message += " with result: " + maskSensitiveData(result);
            }
            logger.debug(message);
        }
    }

    /**
     * Mask sensitive data in logs
     * @param data Data to mask
     * @return Masked data
     */
    private static Object maskSensitiveData(Object data) {
        if (data == null) return null;

        String str = data.toString();

        // Mask passwords
        if (str.toLowerCase().contains("password") || str.toLowerCase().contains("passwd")) {
            return "***MASKED***";
        }

        // Mask card numbers (16 digits)
        if (str.matches(".*\\d{16}.*")) {
            return str.replaceAll("\\b\\d{12}(\\d{4})\\b", "**** **** **** $1");
        }

        // Mask CVV (3-4 digits)
        if (str.matches(".*\\b\\d{3,4}\\b.*")) {
            return str.replaceAll("\\b\\d{3,4}\\b", "***");
        }

        // Mask phone numbers
        if (str.matches(".*\\d{10}.*")) {
            return str.replaceAll("\\b(\\d{3})\\d{3}(\\d{4})\\b", "$1***$2");
        }

        // Mask emails (keep only first and last character of local part)
        if (str.contains("@")) {
            return str.replaceAll("(\\w)[^@]*(@)", "$1***$2");
        }

        return data;
    }

    /**
     * Log error with context
     * @param logger Logger instance
     * @param message Error message
     * @param throwable Exception
     * @param context Additional context
     */
    public static void logError(Logger logger, String message, Throwable throwable, Object... context) {
        StringBuilder sb = new StringBuilder(message);
        if (context.length > 0) {
            sb.append(" | Context: ");
            for (Object obj : context) {
                sb.append(maskSensitiveData(obj)).append(" ");
            }
        }
        logger.error(sb.toString(), throwable);
    }

    /**
     * Log audit trail for financial transactions
     * @param userId User ID
     * @param action Action performed
     * @param amount Transaction amount (if any)
     * @param status Transaction status
     * @param additionalInfo Additional information
     */
    public static void logAudit(String userId, String action, Double amount, String status, String additionalInfo) {
        Logger auditLogger = LogManager.getLogger("AUDIT");
        String logMessage = String.format("AUDIT | User: %s | Action: %s | Amount: %s | Status: %s | Info: %s",
                userId, action, amount != null ? "$" + amount : "N/A", status, additionalInfo);
        auditLogger.info(logMessage);
    }
}