package com.alphawarthog.dbutils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TransactionManager {
	
	private static final int MAX_STATEMENTS = 100;
	
	protected final Logger logger = LogManager.getLogger(getClass());

	private final DataSource dataSource;
	
	public TransactionManager(String url) {
		this(url, null, null);
	}
	
	public TransactionManager(String url, String username, String password) {
		ComboPooledDataSource ds = new ComboPooledDataSource();
		ds.setJdbcUrl(Objects.requireNonNull(url, "JDBC URL cannot be null"));
		if (username != null) {
			ds.setUser(username);
			ds.setPassword(password);
		}
		ds.setMaxStatements(MAX_STATEMENTS);
		
		this.dataSource = ds;
		logger.info("Transaction manager to {} created", url);
	}
	
	public Transaction beginTransaction() throws SQLException {
		return new Transaction(dataSource);
	}
	
	public int executeUpdate(String statement, Object... params) throws SQLException {
		try (Transaction tx = beginTransaction()) {
			return tx.executeUpdate(statement, params);
		}
	}
	
	public ResultSet executeQuery(String statement, Object... params) throws SQLException {
		try (Transaction tx = beginTransaction()) {
			return tx.executeQuery(statement, params);
		}
	}
}
