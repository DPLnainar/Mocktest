package com.examportal.parser.pool;

import org.antlr.v4.runtime.Parser;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration; // Explicitly adding import if not present, but better to add at top. Wait, I can't easily add import at top with this tool unless I include file start.
import java.util.function.Supplier;

/**
 * Parser Pool using Apache Commons Pool2
 * 
 * Provides object pooling for ANTLR parsers to achieve <200ms parsing
 * performance
 * Parsers are expensive to create, so we reuse them
 * 
 * Usage:
 * Parser parser = parserPool.borrowObject();
 * try {
 * // Use parser
 * } finally {
 * parserPool.returnObject(parser);
 * }
 */
public class ParserPool<T extends Parser> {

    private final GenericObjectPool<T> pool;

    public ParserPool(Supplier<T> parserFactory, int maxPoolSize) {
        GenericObjectPoolConfig<T> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxPoolSize);
        config.setMaxIdle(maxPoolSize / 2);
        config.setMinIdle(2);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofMillis(5000));

        this.pool = new GenericObjectPool<>(new ParserObjectFactory<>(parserFactory), config);
    }

    public T borrowObject() throws Exception {
        return pool.borrowObject();
    }

    public void returnObject(T parser) {
        pool.returnObject(parser);
    }

    public void close() {
        pool.close();
    }

    public int getNumActive() {
        return pool.getNumActive();
    }

    public int getNumIdle() {
        return pool.getNumIdle();
    }

    /**
     * Factory for creating parser instances
     */
    private static class ParserObjectFactory<T extends Parser> extends BasePooledObjectFactory<T> {
        private final Supplier<T> parserFactory;

        public ParserObjectFactory(Supplier<T> parserFactory) {
            this.parserFactory = parserFactory;
        }

        @Override
        public T create() {
            return parserFactory.get();
        }

        @Override
        public PooledObject<T> wrap(T parser) {
            return new DefaultPooledObject<>(parser);
        }

        @Override
        public void passivateObject(PooledObject<T> p) {
            // Reset parser state before returning to pool
            T parser = p.getObject();
            parser.reset();
        }
    }
}
