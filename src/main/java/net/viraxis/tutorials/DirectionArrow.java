package net.viraxis.tutorials;

final class DirectionArrow {
    private static final double MIN_LENGTH_SQUARED = 1.0E-6D;
    private static final double VERTICAL_THRESHOLD_RADIANS = Math.PI / 8.0D;

    private DirectionArrow() {}

    static String between(float viewYaw, float viewPitch,
                          double targetX, double targetY, double targetZ) {
        double targetLength = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        if (targetLength * targetLength < MIN_LENGTH_SQUARED) return "↑";

        double targetYaw = Math.toDegrees(Math.atan2(-targetX, targetZ));
        double targetPitch = Math.toDegrees(-Math.asin(Math.max(-1.0D, Math.min(1.0D, targetY / targetLength))));
        double yawDifference = wrapDegrees(targetYaw - viewYaw);
        double pitchDifference = Math.toRadians(targetPitch - viewPitch);
        String horizontal = fromYawDifference(yawDifference);

        if (pitchDifference >= VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↙";
                case "→", "↗", "↘" -> "↘";
                default -> "↓";
            };
        }
        if (pitchDifference <= -VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↖";
                case "→", "↗", "↘" -> "↗";
                default -> "↑";
            };
        }
        return horizontal;
    }

    static String between(double viewX, double viewY, double viewZ,
                          double targetX, double targetY, double targetZ) {
        double viewLength = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        double targetLength = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        if (viewLength * viewLength < MIN_LENGTH_SQUARED || targetLength * targetLength < MIN_LENGTH_SQUARED)
            return "↑";

        viewX /= viewLength;
        viewY /= viewLength;
        viewZ /= viewLength;
        targetX /= targetLength;
        targetY /= targetLength;
        targetZ /= targetLength;

        double pitchDifference = Math.asin(Math.max(-1.0D, Math.min(1.0D, targetY)))
                - Math.asin(Math.max(-1.0D, Math.min(1.0D, viewY)));
        String horizontal = between(viewX, viewZ, targetX, targetZ);
        if (pitchDifference <= -VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↙";
                case "→", "↗", "↘" -> "↘";
                default -> "↓";
            };
        }
        if (pitchDifference >= VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↖";
                case "→", "↗", "↘" -> "↗";
                default -> "↑";
            };
        }

        return horizontal;
    }

    static String between(double viewX, double viewZ, double targetX, double targetZ) {
        double viewLength = Math.hypot(viewX, viewZ);
        double targetLength = Math.hypot(targetX, targetZ);
        if (viewLength * viewLength < MIN_LENGTH_SQUARED || targetLength * targetLength < MIN_LENGTH_SQUARED)
            return "↑";

        viewX /= viewLength;
        viewZ /= viewLength;
        targetX /= targetLength;
        targetZ /= targetLength;
        double dot = Math.max(-1.0D, Math.min(1.0D, viewX * targetX + viewZ * targetZ));
        double cross = viewX * targetZ - viewZ * targetX;
        return fromDirection((int) Math.round(Math.atan2(cross, dot) / (Math.PI / 4.0D)));
    }

    private static String fromYawDifference(double yawDifference) {
        return fromDirection((int) Math.round(yawDifference / 45.0D));
    }

    private static String fromDirection(int direction) {
        return switch (direction) {
            case 1 -> "↗";
            case 2 -> "→";
            case 3 -> "↘";
            case 4, -4 -> "↓";
            case -3 -> "↙";
            case -2 -> "←";
            case -1 -> "↖";
            default -> "↑";
        };
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) wrapped -= 360.0D;
        if (wrapped < -180.0D) wrapped += 360.0D;
        return wrapped;
    }
}
