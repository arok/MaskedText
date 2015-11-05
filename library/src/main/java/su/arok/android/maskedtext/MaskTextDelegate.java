package su.arok.android.maskedtext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: Sergey Chuvashev
 * Date: 02/05/15
 * Time: 22:20
 */
public class MaskTextDelegate implements TextWatcher {

    private EditText mEditText;

    private String mMask;
    private char mCharRepresentation;
    private String mAllowedChars;
    private String mDeniedChars;

    private int[] mRawToMask;
    private RawText mRawText;
    private boolean mEditingBefore;
    private boolean mEditingOnChanged;
    private boolean mEditingAfter;
    private int[] mMaskToRaw;
    private int mSelection;
    private boolean mInitialized;
    private boolean mIgnore;
    protected int mMaxRawLength;
    private int mLastValidMaskPosition;
    private boolean mSelectionChanged;

    public MaskTextDelegate(@NonNull EditText editText,@Nullable String mask, @Nullable String representation) {
        this(editText, mask, representation, null, null);
    }

    public MaskTextDelegate(EditText editText, @Nullable String mask, @Nullable String representation,
                            @Nullable String allowedChars, @Nullable String deniedChars) {
        mMask = mask;
        mEditText = editText;
        this.mAllowedChars = allowedChars;
        this.mDeniedChars = deniedChars;

        editText.addTextChangedListener(this);

        if (representation == null) {
            mCharRepresentation = '#';
        } else {
            mCharRepresentation = representation.charAt(0);
        }

        cleanUp();

        // Ignoring enter key presses
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_NEXT:
                    case EditorInfo.IME_ACTION_DONE:
                        return false;
                    default:
                        return true;
                }
            }
        });
    }

    private void cleanUp() {
        if (mMask == null)
            return;

        mInitialized = false;

        generatePositionArrays();

        mRawText = new RawText();
        mSelection = mRawToMask[0];

        mEditingBefore = true;
        mEditingOnChanged = true;
        mEditingAfter = true;
        if (hasHint()) {
            mEditText.setText(null);
        } else {
            mEditText.setText(mMask.replace(mCharRepresentation, ' '));
        }
        mEditingBefore = false;
        mEditingOnChanged = false;
        mEditingAfter = false;

        mMaxRawLength = mMaskToRaw[previousValidPosition(mMask.length() - 1)] + 1;
        mLastValidMaskPosition = findLastValidMaskPosition();
        mInitialized = true;

        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mEditText.hasFocus() && (mRawText.length() > 0 || !hasHint())) {
                    mSelectionChanged = false;
                    mEditText.setSelection(lastValidPosition());
                }
            }
        });
    }

    public void setMask(@Nullable String mask) {
        mMask = mask;
        cleanUp();
    }

    public @Nullable String getMask() {
        return mMask;
    }

    private void generatePositionArrays() {
        int length = mMask.length();
        int[] aux = new int[length];
        mMaskToRaw = new int[length];

        int charIndex = 0;
        for (int i = 0; i < length; i++) {
            char currentChar = mMask.charAt(i);
            if (currentChar == mCharRepresentation) {
                aux[charIndex] = i;
                mMaskToRaw[i] = charIndex++;
            } else {
                mMaskToRaw[i] = -1;
            }
        }

        mRawToMask = new int[charIndex];
        System.arraycopy(aux, 0, mRawToMask, 0, charIndex);
    }

    private boolean hasHint() {
        return mEditText.getHint() != null;
    }

    private int fixSelection(int selection) {
        if (selection > lastValidPosition()) {
            return lastValidPosition();
        } else {
            return nextValidPosition(selection);
        }
    }

    private int nextValidPosition(int currentPosition) {
        while (currentPosition < mLastValidMaskPosition && mMaskToRaw[currentPosition] == -1) {
            currentPosition++;
        }
        if (currentPosition > mLastValidMaskPosition) return mLastValidMaskPosition + 1;
        return currentPosition;
    }

    private int previousValidPosition(int currentPosition) {
        while (currentPosition >= 0 && mMaskToRaw[currentPosition] == -1) {
            currentPosition--;
            if (currentPosition < 0) {
                return nextValidPosition(0);
            }
        }
        return currentPosition;
    }

    private int lastValidPosition() {
        if (mRawText.length() == mMaxRawLength) {
            return mRawToMask[mRawText.length() - 1] + 1;
        }
        return nextValidPosition(mRawToMask[mRawText.length()]);
    }

    private String makeMaskedText() {
        char[] maskedText = mMask.replace(mCharRepresentation, ' ').toCharArray();
        for (int i = 0; i < mRawToMask.length; i++) {
            if (i < mRawText.length()) {
                maskedText[mRawToMask[i]] = mRawText.charAt(i);
            } else {
                maskedText[mRawToMask[i]] = ' ';
            }
        }
        return new String(maskedText);
    }

    private Range calculateRange(int start, int end) {
        Range range = new Range();
        for (int i = start; i <= end && i < mMask.length(); i++) {
            if (mMaskToRaw[i] != -1) {
                if (range.getStart() == -1) {
                    range.setStart(mMaskToRaw[i]);
                }
                range.setEnd(mMaskToRaw[i]);
            }
        }
        if (end == mMask.length()) {
            range.setEnd(mRawText.length());
        }
        if (range.getStart() == range.getEnd() && start < end) {
            int newStart = previousValidPosition(range.getStart() - 1);
            if (newStart < range.getStart()) {
                range.setStart(newStart);
            }
        } else if (range.getStart() > range.getEnd()) {
            range.setStart(range.getEnd());
        }
        return range;
    }

    private String clear(String string) {
        if (mDeniedChars != null) {
            for (char c : mDeniedChars.toCharArray()) {
                string = string.replace(Character.toString(c), "");
            }
        }

        if (mAllowedChars != null) {
            StringBuilder builder = new StringBuilder(string.length());
            char[] chars = string.toCharArray();

            for (char c : string.toCharArray()) {
                if (mAllowedChars.contains(String.valueOf(c))) {
                    builder.append(c);
                }
            }

            string = builder.toString();
        }

        return string;
    }

    private int findLastValidMaskPosition() {
        for (int i = mMaskToRaw.length - 1; i >= 0; i--) {
            if (mMaskToRaw[i] != -1) return i;
        }
        throw new RuntimeException("Mask contains only the representation char");
    }

    private int erasingStart(int start) {
        while (start > 0 && start < mMaskToRaw.length && mMaskToRaw[start] == -1) {
            start--;
        }
        return start;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mMask == null) return;

        if (!mEditingBefore) {
            mEditingBefore = true;
            if (start > mLastValidMaskPosition) {
                mIgnore = true;
            }
            int rangeStart = start;
            if (after == 0) {
                rangeStart = erasingStart(start);
            }
            Range range = calculateRange(rangeStart, start + count);
            if (range.getStart() != -1) {
                mRawText.subtractFromString(range);
            }
            if (count > 0) {
                mSelection = previousValidPosition(start);
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mMask == null) return;

        if (!mEditingOnChanged && mEditingBefore) {
            mEditingOnChanged = true;
            if (mIgnore) {
                return;
            }
            if (count > 0) {
                int startingPosition = mMaskToRaw[nextValidPosition(start)];
                String addedString = s.subSequence(start, start + count).toString();
                count = mRawText.addToString(clear(addedString), startingPosition, mMaxRawLength);
                if (mInitialized && count >= 0) {
                    int currentPosition;
                    if (startingPosition + count < mRawToMask.length)
                        currentPosition = mRawToMask[startingPosition + count];
                    else
                        currentPosition = mLastValidMaskPosition + 1;
                    mSelection = nextValidPosition(currentPosition);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mMask == null) return;

        if (!mEditingAfter && mEditingBefore && mEditingOnChanged) {
            mEditingAfter = true;
            if (mRawText.length() == 0 && hasHint()) {
                mSelection = 0;
                mEditText.setText(null);
            } else {
                mEditText.setText(makeMaskedText());
            }

            mSelectionChanged = false;
            mEditText.setSelection(mSelection);

            mEditingBefore = false;
            mEditingOnChanged = false;
            mEditingAfter = false;
            mIgnore = false;
        }
    }

    public void onSelectionChanged(int selStart, int selEnd) {
        if (mMask == null) return;

        // On Android 4+ this method is being called more than 1 time if there is a hint in the EditText, what moves the cursor to left
        // Using the boolean var selectionChanged to limit to one execution
        if (mInitialized) {
            if (!mSelectionChanged) {
                if (mRawText.length() == 0 && hasHint()) {
                    selStart = 0;
                    selEnd = 0;
                } else {
                    selStart = fixSelection(selStart);
                    selEnd = fixSelection(selEnd);
                }
                mEditText.setSelection(selStart, selEnd);
                mSelectionChanged = true;
            } else {
                //check to see if the current selection is outside the already entered text
                if (!(hasHint() && mRawText.length() == 0) && selStart > mRawText.length() - 1) {
                    mEditText.setSelection(fixSelection(selStart), fixSelection(selEnd));
                }
            }
        }
    }

}
