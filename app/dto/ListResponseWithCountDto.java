package dto;

import java.util.List;

/**
 * Created by yakov_000 on 13.02.2015.
 */
public class ListResponseWithCountDto extends ListResponseDto<ProductDto>{

    private Long count;

    public ListResponseWithCountDto(List<ProductDto> data, Long count) {
        super(data);
        this.count=count;
    }

    public Long getCount() {
        return count;
    }
}
