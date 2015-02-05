package db;

import com.github.pgasync.ConnectionPoolBuilder;
import  com.github.pgasync.ConnectionPool;

/**
 * Created by yakov_000 on 29.01.2015.
 */
public class MyConnectionPool {
    public static final ConnectionPool db = new ConnectionPoolBuilder().hostname("localhost").port(5432).database("myshop").username("myshop").password("myshop").poolSize(20).build();

}
