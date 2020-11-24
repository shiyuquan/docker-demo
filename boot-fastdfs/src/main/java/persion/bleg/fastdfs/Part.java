package persion.bleg.fastdfs;

import lombok.Data;

/**
 * 文件部分
 *
 * @author shiyuquan
 * @since 2020/5/9 2:43 下午
 */
@Data
public class Part {
    /** 文件的块第几块，从1开始 */
    private Integer number;

    /** 当前块的保存路径 */
    private String uri;
    private String groupName;
    private String fileId;

    /** 块所属文件的原始文件名 */
    private String origFileName;

    /** 原文件的唯一标识，用来判断块是属于哪个文件 */
    private String identifier;

    /** 分块大小 */
    private Integer partSize;

    /** 当前分块大小 */
    private Long currentSize;

    /** 原始文件总大小 */
    private Long totalSize;

    /** 总块数 */
    private Integer totalChunks;

    /** 文件类型 */
    private String type;

    /** 当前分片文件在原始文件的字节数组起始下标。文件分块大小固定时，当前值为 number * partSize。 */
    private Long beginPoint;
}
