package net.viraxis.tutorials;

final class DirectionArrow {
    private static final double MIN_LENGTH_SQUARED = 1.0E-6D;

    private DirectionArrow() {}

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
        int direction = (int) Math.round(Math.atan2(cross, dot) / (Math.PI / 4.0D));
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
}
