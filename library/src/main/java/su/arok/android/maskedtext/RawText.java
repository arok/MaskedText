package su.arok.android.maskedtext;

class RawText {

    private String mText;

    RawText() {
        mText = "";
    }

    void subtractFromString(Range range) {
        String firstPart = "";
        String lastPart = "";

        if (range.getStart() > 0 && range.getStart() <= mText.length()) {
            firstPart = mText.substring(0, range.getStart());
        }
        if (range.getEnd() >= 0 && range.getEnd() < mText.length()) {
            lastPart = mText.substring(range.getEnd(), mText.length());
        }
        mText = firstPart.concat(lastPart);
    }

    /**
     * @param newString New String to be added
     * @param start     Position to insert newString
     * @param maxLength Maximum raw text length
     * @return Number of added characters
     */
    int addToString(String newString, int start, int maxLength) {
        String firstPart = "";
        String lastPart = "";

        if (newString == null || newString.equals("")) {
            return 0;
        } else if (start < 0) {
            return -1;
        } else if (start > mText.length()) {
            return -1;
        }

        int count = newString.length();

        if (start > 0) {
            firstPart = mText.substring(0, start);
        }
        if (start >= 0 && start < mText.length()) {
            lastPart = mText.substring(start, mText.length());
        }
        if (mText.length() + newString.length() > maxLength) {
            count = maxLength - mText.length();
            newString = newString.substring(0, count);
        }
        mText = firstPart.concat(newString).concat(lastPart);
        return count;
    }

    String getText() {
        return mText;
    }

    int length() {
        return mText.length();
    }

    char charAt(int position) {
        return mText.charAt(position);
    }
}
