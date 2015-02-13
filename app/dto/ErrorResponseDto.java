package dto;

/**
 * Created by yakov_000 on 12.02.2015.
 */
public class ErrorResponseDto {
    private String error;

    public ErrorResponseDto(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
