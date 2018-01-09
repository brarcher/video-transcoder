package protect.videotranscoder;

/**
 * A callback when an async call completes and has a result to return.
 * @param <Type>
 */
public interface ResultCallbackHandler<Type>
{
    void onResult(Type result);
}
