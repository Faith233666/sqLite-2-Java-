package sqlite;

/**
 * users 表对应的实体类。
 */
public record User(Long id, String name, int age) {

    /** 新增用户时尚未分配 id。 */
    public User(String name, int age) {
        this(null, name, age);
    }
}
