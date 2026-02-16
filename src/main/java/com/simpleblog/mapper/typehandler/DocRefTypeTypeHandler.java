package com.simpleblog.mapper.typehandler;

import com.simpleblog.model.entity.DocRefType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(DocRefType.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class DocRefTypeTypeHandler extends BaseTypeHandler<DocRefType> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DocRefType parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject value = new PGobject();
        value.setType("doc_ref_type");
        value.setValue(parameter.getValue());
        ps.setObject(i, value);
    }

    @Override
    public DocRefType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return DocRefType.fromValue(rs.getString(columnName));
    }

    @Override
    public DocRefType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return DocRefType.fromValue(rs.getString(columnIndex));
    }

    @Override
    public DocRefType getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        return DocRefType.fromValue(cs.getString(columnIndex));
    }
}
