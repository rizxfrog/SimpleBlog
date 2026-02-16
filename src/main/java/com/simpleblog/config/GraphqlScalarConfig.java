package com.simpleblog.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.language.StringValue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphqlScalarConfig {
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType dateTimeScalar = GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("LocalDateTime scalar mapped to DateTime in schema")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalDateTime localDateTime) {
                            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        if (dataFetcherResult instanceof OffsetDateTime offsetDateTime) {
                            return offsetDateTime.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime or OffsetDateTime.");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) {
                        if (input instanceof String text) {
                            try {
                                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (Exception ex) {
                                throw new CoercingParseValueException("Invalid DateTime value.", ex);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String.");
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return LocalDateTime.parse(stringValue.getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (Exception ex) {
                                throw new CoercingParseLiteralException("Invalid DateTime literal.", ex);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue.");
                    }
                })
                .build();
        return wiringBuilder -> wiringBuilder
                .scalar(dateTimeScalar)
                .scalar(ExtendedScalars.GraphQLLong);
    }
}
