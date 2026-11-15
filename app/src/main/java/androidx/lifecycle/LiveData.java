package androidx.lifecycle;

public class LiveData<T> {
    private volatile T mData;
    private volatile ValueChangeListener<T> mListener;

    public interface ValueChangeListener<T> {
        void onChanged(T value);
    }

    public LiveData() {}

    public LiveData(T value) {
        mData = value;
    }

    public T getValue() {
        return mData;
    }

    public void setChangeListener(ValueChangeListener<T> listener) {
        this.mListener = listener;
    }

    public void postValue(T value) {
        mData = value;
        ValueChangeListener<T> l = mListener;
        if (l != null) {
            try { l.onChanged(value); } catch (Throwable ignored) {}
        }
    }

    public void setValue(T value) {
        mData = value;
        ValueChangeListener<T> l = mListener;
        if (l != null) {
            try { l.onChanged(value); } catch (Throwable ignored) {}
        }
    }
}
