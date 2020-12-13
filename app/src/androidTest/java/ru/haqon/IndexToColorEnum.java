package ru.haqon;

public enum IndexToColorEnum {
    BLACK(0), BROWN(1), RED(2), ORANGE(3), YELLOW(4), GREEN(5), BLUE(6), PURPLE(7), GRAY(7), WHITE(8);

    private int index;

    IndexToColorEnum(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static IndexToColorEnum getColorByIndex(final int index) {
        for (IndexToColorEnum c : IndexToColorEnum.values()) {
            if (c.index == index) return c;
        }
        return null;
    }
}
