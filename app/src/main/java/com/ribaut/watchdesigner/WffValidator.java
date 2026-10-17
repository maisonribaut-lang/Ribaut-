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
            var validator = DwfValidatorFactory.create();
            var validation = validator.validate(apk, marketplacePackage);
            if (!validation.failures().isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (var failure : validation.failures()) {
                    if (details.length() > 0) details.append("\n");
                    details.append(failure.name()).append(": ").append(failure.failureMessage());
                }
                return Result.error(details.toString());
            }
            return Result.ok(validation.validationToken());
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
