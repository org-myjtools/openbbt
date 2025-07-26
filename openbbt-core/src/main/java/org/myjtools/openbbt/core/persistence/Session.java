package org.myjtools.openbbt.core.persistence;


import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.util.Log;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.*;
import java.util.stream.*;

public class Session implements AutoCloseable {

    private static final Pattern PARAM_REGEX = Pattern.compile("\\?\\d\\d?");


    @FunctionalInterface
    public interface Mapper<T> {
        T mapThrowing(ResultSet resultSet) throws Exception;
        default T map (ResultSet resultSet) {
            try {
                return mapThrowing(resultSet);
            } catch (Exception e) {
                throw new OpenBBTException(e);
            }
        }
    }

    private record ParsedSQL (String sql, int[] argIndexes) {}

    private final Log log;
    private final Supplier<Connection> connectionProvider;
    private final Map<String, PreparedStatement> statements = new HashMap<>();
    private final Map<String, ParsedSQL> parsedSQL = new HashMap<>();
    private Connection connection;

    public Session(Supplier<Connection> connectionProvider, Log log) {
        this.connectionProvider = connectionProvider;
        this.log = log;
    }


    public void commit() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    @Override
    public void close() {
        try {
            for (PreparedStatement statement : statements.values()) {
                if (!statement.isClosed()) {
                    statement.close();
                }
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            statements.clear();
            parsedSQL.clear();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    private Connection connection() {
        try {
            if (connection == null || connection.isClosed()) {
                statements.clear();
                connection = connectionProvider.get();
                connection.setAutoCommit(false);
            }
            return connection;
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    private PreparedStatement statement(String sql, Object... args) {
        try {
            ParsedSQL parsed = parsedSQL.computeIfAbsent(sql, this::parseSql);
            log.trace("[SQL] <<{}>> using arguments {}" ,()->parsed.sql, ()->Arrays.toString(args));
            PreparedStatement cached = statements.get(parsed.sql);
            if (cached == null || cached.isClosed()) {
                cached = connection().prepareStatement(parsed.sql);
                statements.put(parsed.sql, cached);
            }
            for (int i = 0; i < parsed.argIndexes.length; i++) {
                cached.setObject(i+1, args[parsed.argIndexes[i]]);
            }

            return cached;
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    /* replace the ?1, ?2, etc. by simple ? and annotate the actual argument index to use */
    private ParsedSQL parseSql (String sql) {
        List<Integer> argIndexes = new LinkedList<>();
        Matcher paramMatcher = PARAM_REGEX.matcher(sql);
        while (paramMatcher.find()) {
            int index = Integer.parseInt(paramMatcher.group().substring(1));
            argIndexes.add(index-1);
        }
        String actualSql = PARAM_REGEX.matcher(sql)
                .replaceAll("?")
                .replaceAll("\\n"," ")
                .replaceAll("\\s+"," ");
        return new ParsedSQL(actualSql, argIndexes.stream().mapToInt(Integer::intValue).toArray());
    }


    public <T> Stream<T> stream(Mapper<T> mapper, String sql, Object... args) {
        try {
            var resultSet = statement(sql,args).executeQuery();
            return stream(resultSet,mapper);
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public <T> List<T> list(Mapper<T> mapper, String sql, Object... args) {
        try (var resultSet = statement(sql,args).executeQuery()) {
            List<T> result = new LinkedList<>();
            while (resultSet.next()) {
                result.add(mapper.map(resultSet));
            }
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public <T> Optional<T> optional(Mapper<T> mapper, String sql, Object... args) {
        try (var resultSet = statement(sql,args).executeQuery()) {
            return resultSet.next() ? Optional.of(mapper.map(resultSet)) : Optional.empty();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public <T> T single(Mapper<T> mapper, String sql, Object... args) {
        try (var resultSet = statement(sql,args).executeQuery()) {
            resultSet.next();
            return mapper.map(resultSet);
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }

    public boolean exists(String sql, Object... args) {
        try (var resultSet = statement(sql,args).executeQuery()) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public void execute(List<String> sentences) {
        try (var statement = connection().createStatement()) {
            for (String sentence : sentences) {
                statement.addBatch(sentence);
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public void execute(String sql, Object... args) {
        try {
            statement(sql,args).executeUpdate();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public void executeBatch(String sql, List<List<?>> argsSet) {
        try {
            ParsedSQL parsed = parsedSQL.computeIfAbsent(sql, this::parseSql);
            log.trace("[SQL] <<{}>> using arguments {}", parsed.sql, argsSet);
            PreparedStatement cached = statements.get(parsed.sql);
            if (cached == null || cached.isClosed()) {
                cached = connection().prepareStatement(parsed.sql);
                statements.put(parsed.sql, cached);
            }
            for (List<?> args : argsSet) {
                for (int i = 0; i < parsed.argIndexes.length; i++) {
                    cached.setObject(i + 1, args.get(parsed.argIndexes[i]));
                }
                cached.addBatch();
            }
            cached.executeBatch();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }




    private <T> Stream<T> stream(ResultSet resultSet, Mapper<T> mapper) {
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(
                                new ResultSetIterator<>(resultSet, mapper),
                                Spliterator.ORDERED
                        ), false)
                .onClose(() -> {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        throw new OpenBBTException(e);
                    }
                });
    }


    private record ResultSetIterator<T>(ResultSet resultSet, Mapper<T> mapper)
            implements Iterator<T> {

        @Override
        public boolean hasNext() {
            try {
                return resultSet.next();
            } catch (SQLException e) {
                throw new OpenBBTException(e);
            }
        }

        @Override
        public T next() {
            return mapper.map(resultSet);
        }


        @Override
        public void remove() {
            try {
                resultSet.deleteRow();
            } catch (SQLException e) {
                throw new OpenBBTException(e);
            }
        }

    }



}
