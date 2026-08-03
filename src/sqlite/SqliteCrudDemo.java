package sqlite;

import java.sql.SQLException;
import java.util.List;

/**
 * SQLite + JDBC CRUD 完整示例（Java 21）。
 */
public class SqliteCrudDemo {

    public static void main(String[] args) {
        UserDao userDao = new UserDao();

        try {
            // 1. 初始化表结构
            userDao.initTable();
            System.out.println("=== 1. 建表完成 ===");

            // 2. 增（Create）
            long aliceId = userDao.insert(new User("Alice", 25));
            long bobId = userDao.insert(new User("Bob", 30));
            System.out.println("=== 2. 新增用户 ===");
            System.out.println("Alice id = " + aliceId);
            System.out.println("Bob   id = " + bobId);

            // 3. 查（Read）— 按 id 查询
            System.out.println("\n=== 3. 按 id 查询 ===");
            userDao.findById(aliceId).ifPresentOrElse(
                    user -> System.out.println("找到: " + formatUser(user)),
                    () -> System.out.println("未找到 id = " + aliceId)
            );

            // 4. 查（Read）— 查询全部
            System.out.println("\n=== 4. 查询全部用户 ===");
            printAllUsers(userDao.findAll());

            // 5. 改（Update）
            System.out.println("\n=== 5. 更新用户 ===");
            boolean updated = userDao.update(new User(aliceId, "Alice Wang", 26));
            System.out.println(updated ? "更新成功" : "更新失败");
            printAllUsers(userDao.findAll());

            // 6. 删（Delete）
            System.out.println("\n=== 6. 删除用户 ===");
            boolean deleted = userDao.deleteById(bobId);
            System.out.println(deleted ? "删除 Bob 成功" : "删除 Bob 失败");
            printAllUsers(userDao.findAll());

        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printAllUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("（暂无数据）");
            return;
        }
        for (User user : users) {
            System.out.println(formatUser(user));
        }
    }

    private static String formatUser(User user) {
        return "User{id=" + user.id() + ", name='" + user.name() + "', age=" + user.age() + "}";
    }
}
