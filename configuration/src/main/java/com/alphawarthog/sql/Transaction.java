package com.alphawarthog.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Transaction implements AutoCloseable {
	
	protected final Logger logger = LogManager.getLogger(getClass());
	
	private final Connection conn;
	private final Map<String, PreparedStatement> statementMap = new HashMap<>();

	protected Transaction(DataSource ds) throws SQLException {
		this.conn = ds.getConnection();
		this.conn.setAutoCommit(false);
		logger.debug("Transaction started");
	}
	
	@Override
	public void close() throws SQLException {
		try {
			conn.commit();
			logger.debug("Transaction committed");
		} catch (SQLException e) {
			conn.rollback();
			logger.error("Transaction rolled back: {}", e.getMessage(), e);
			throw e;
		} finally {
			try {
				conn.setAutoCommit(true);
				conn.close();
				logger.debug("Transaction ended");
			} catch (SQLException e) {
				logger.error("Unable to end transaction: {}", e.getMessage(), e);
			}
		} 
	}
	
	private PreparedStatement prepareStatement(String statement, Object... params) throws SQLException {
		PreparedStatement ps = statementMap.computeIfAbsent(statement, t -> {
			try {
				return conn.prepareStatement(t);
			} catch (SQLException e) {
				logger.error("Unable to prepare statement {}: {}", statement, e.getMessage(), e);
			}
			
			return null;
		});
		
		if (ps == null) {
			throw new SQLException("Unable to prepare statement " + statement);
		}
		
		for (int i = 0; i < params.length; i++) {
			ps.setObject(i + 1, params[i]);
		}
		
		logger.debug("Statement {} prepared", statement);
		return ps;
	}
	
	public int[] executeBatch(String statement) throws SQLException {
		PreparedStatement ps = statementMap.get(statement);
		if (ps == null) {
			throw new SQLException("Statement " + statement + " has not been prepared");
		}
		
		int[] result = ps.executeBatch();
		logger.debug("{} rows affected by batch execution of statement {}", () -> Arrays.stream(result).sum(), () -> statement);
		return result;
	}
	
	public void addBatch(String statement, Object... params) throws SQLException {
		PreparedStatement ps = prepareStatement(statement, params);
		ps.addBatch();
		logger.debug("Batch added to statement {}", statement);
	}
	
	public int executeUpdate(String statement, Object... params) throws SQLException {
		PreparedStatement ps = prepareStatement(statement, params);
		int result = ps.executeUpdate();
		logger.debug("{} rows affected by executing statement {}", result, statement);
		return result;
	}
	
	public ResultSet executeQuery(String statement, Object... params) throws SQLException {
		PreparedStatement ps = prepareStatement(statement, params);
		ResultSet rs = ps.executeQuery();
		logger.debug("Query {} executed", statement);
		return rs;
	}
}
