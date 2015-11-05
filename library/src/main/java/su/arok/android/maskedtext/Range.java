package su.arok.android.maskedtext;

class Range {

    private int mStart;
    private int mEnd;

    Range() {
        mStart = -1;
        mEnd = -1;
    }

    int getStart() {
        return mStart;
    }

    void setStart(int start) {
        this.mStart = start;
    }

    int getEnd() {
        return mEnd;
    }

    void setEnd(int end) {
        this.mEnd = end;
    }

}
