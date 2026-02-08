package com.simpleblog.mapper.typehandler;

import com.simpleblog.model.entity.DocumentNodeType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(DocumentNodeType.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class DocumentNodeTypeTypeHandler extends BaseTypeHandler<DocumentNodeType> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DocumentNodeType parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject value = new PGobject();
        value.setType("doc_node_type");
        value.setValue(parameter.getValue());
        ps.setObject(i, value);
    }

    @Override
    public DocumentNodeType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return DocumentNodeType.fromValue(rs.getString(columnName));
    }

    @Override
    public DocumentNodeType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return DocumentNodeType.fromValue(rs.getString(columnIndex));
    }

    @Override
    public DocumentNodeType getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        return DocumentNodeType.fromValue(cs.getString(columnIndex));
    }
}
