package sqlite;

/**
 * 统一 API 响应格式，与前端 axios 封装保持一致。
 */
public record ApiResponse<T>(int code, String msg, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static ApiResponse<Void> success(String msg) {
        return new ApiResponse<>(200, msg, null);
    }

    public static ApiResponse<Void> error(String msg) {
        return new ApiResponse<>(500, msg, null);
    }

    public static ApiResponse<Void> badRequest(String msg) {
        return new ApiResponse<>(400, msg, null);
    }
}
