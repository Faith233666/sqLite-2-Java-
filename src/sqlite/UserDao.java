package sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JDBC 的 users 表 CRUD 操作。
 * 所有 SQL 均通过 PreparedStatement 执行，避免 SQL 注入。
 */
public class UserDao {

    private static final String JDBC_URL = "jdbc:sqlite:" + initDbPath();

    private static String initDbPath() {
        String path = System.getenv("DB_PATH");
        if (path == null || path.isBlank()) {
            path = System.getProperty("db.path", "data/demo.db");
        }
        try {
            Path dbFile = Path.of(path).toAbsolutePath().normalize();
            Path parent = dbFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return dbFile.toString();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("无法初始化数据库路径: " + path);
        }
    }

    static {
        try {
            // 显式加载 SQLite JDBC 驱动
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** 初始化 users 表（若不存在则创建）。 */
    public void initTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT    NOT NULL,
                    age  INTEGER NOT NULL
                )
                """;

        // try-with-resources 自动关闭 Connection 和 Statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /** 新增用户，返回数据库生成的主键 id。 */
    public long insert(User user) throws SQLException {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.name());
            ps.setInt(2, user.age());
            ps.executeUpdate();

            // 读取自增主键
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("插入成功，但未获取到自增 id");
            }
        }
    }

    /** 按 id 查询单个用户。 */
    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT id, name, age FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /** 查询全部用户。 */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, name, age FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    /** 按 id 更新用户姓名和年龄，返回是否更新成功。 */
    public boolean update(User user) throws SQLException {
        if (user.id() == null) {
            throw new IllegalArgumentException("更新时 id 不能为空");
        }

        String sql = "UPDATE users SET name = ?, age = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.name());
            ps.setInt(2, user.age());
            ps.setLong(3, user.id());

            return ps.executeUpdate() > 0;
        }
    }

    /** 按 id 删除用户，返回是否删除成功。 */
    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("age")
        );
    }
}
