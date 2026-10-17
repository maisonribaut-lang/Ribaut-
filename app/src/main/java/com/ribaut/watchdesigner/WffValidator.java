package com.ribaut.watchdesigner;

import com.google.android.wearable.watchface.validator.client.DwfValidatorFactory;
import java.io.File;

/** Validates a generated WFF APK on the phone and returns the token required by Watch Face Push. */
public final class WffValidator {
    private WffValidator() {}

    public static Result validate(File apk, String marketplacePackage) {
        if (apk == null || !apk.isFile() || apk.length() == 0) {
            return Result.error("APK WFF absent ou vide");
        }
        try {
            Object validator = DwfValidatorFactory.create();
            java.lang.reflect.Method validate = validator.getClass().getMethod("validate", File.class, String.class);
            Object validation = validate.invoke(validator, apk, marketplacePackage);
            java.lang.reflect.Method failuresMethod = validation.getClass().getMethod("failures");
            Iterable<?> failures = (Iterable<?>) failuresMethod.invoke(validation);
            StringBuilder details = new StringBuilder();
            for (Object failure : failures) {
                if (details.length() > 0) details.append("\n");
                java.lang.reflect.Method name = failure.getClass().getMethod("name");
                java.lang.reflect.Method message = failure.getClass().getMethod("failureMessage");
                details.append(String.valueOf(name.invoke(failure))).append(": ")
                        .append(String.valueOf(message.invoke(failure)));
            }
            if (details.length() > 0) return Result.error(details.toString());
            java.lang.reflect.Method tokenMethod = validation.getClass().getMethod("validationToken");
            return Result.ok(String.valueOf(tokenMethod.invoke(validation)));
        } catch (Throwable t) {
            return Result.error(t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
        }
    }

    public static final class Result {
        public final boolean ok;
        public final String token;
        public final String error;

        private Result(boolean ok, String token, String error) {
            this.ok = ok;
            this.token = token;
            this.error = error;
        }

        static Result ok(String token) { return new Result(true, token, null); }
        static Result error(String error) { return new Result(false, null, error); }
    }
}
