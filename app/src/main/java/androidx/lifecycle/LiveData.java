package androidx.lifecycle;

public class LiveData<T> {
    private volatile T mData;

    public LiveData() {}

    public LiveData(T value) {
        mData = value;
    }

    public T getValue() {
        return mData;
    }

    protected void postValue(T value) {
        mData = value;
    }

    protected void setValue(T value) {
        mData = value;
    }
}
