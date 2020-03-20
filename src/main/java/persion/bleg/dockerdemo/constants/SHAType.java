package persion.bleg.dockerdemo.constants;

import lombok.AllArgsConstructor;

/**
 * SHA加密类型
 *
 * @author shiyuquan
 * @since 2020/3/9 12:40 下午
 */
@AllArgsConstructor
public enum SHAType {

    /**
     * type
     */
    SHA224("sha-224"),
    SHA256("sha-256"),
    SHA384("sha-384"),
    SHA512("sha-512"),
    ;

    public String value;

    /**
     * 通过value获取枚举
     * @param value value
     * @return SHAType
     */
    public static SHAType getByValue(String value) {
        for (SHAType o : values()) {
            if (value.equals(o.value)) {
                return o;
            }
        }
        return null;
    }
}
