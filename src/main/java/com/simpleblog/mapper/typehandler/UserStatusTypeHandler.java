package com.simpleblog.mapper.typehandler;

import com.simpleblog.model.entity.UserStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(UserStatus.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class UserStatusTypeHandler extends BaseTypeHandler<UserStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserStatus parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject value = new PGobject();
        value.setType("user_status");
        value.setValue(parameter.getValue());
        ps.setObject(i, value);
    }

    @Override
    public UserStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return UserStatus.fromValue(rs.getString(columnName));
    }

    @Override
    public UserStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return UserStatus.fromValue(rs.getString(columnIndex));
    }

    @Override
    public UserStatus getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        return UserStatus.fromValue(cs.getString(columnIndex));
    }
}
