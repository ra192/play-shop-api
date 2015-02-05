package dto;

import java.util.List;

/**
 * Created by yakov_000 on 05.02.2015.
 */
public class ListResponseDto<T> {

    private List<T>data;

    public ListResponseDto(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return data;
    }
}
