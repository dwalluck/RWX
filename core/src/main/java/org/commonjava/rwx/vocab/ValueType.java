/**
 * Copyright (C) 2010 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.rwx.vocab;

import org.apache.commons.codec.binary.Base64;
import org.commonjava.rwx.error.CoercionException;
import org.commonjava.rwx.util.ValueCoercion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public enum ValueType
{
    STRUCT( new ValueCoercion()
    {
        @Override
        public String toString( final Object value )
            throws CoercionException
        {
            throw new CoercionException( "Cannot coerce STRUCT types." );
        }

        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            throw new CoercionException( "Cannot coerce STRUCT types." );
        }
    }, Map.class, XmlRpcConstants.STRUCT ),

    ARRAY( new ValueCoercion()
    {
        @Override
        public String toString( final Object value )
            throws CoercionException
        {
            throw new CoercionException( "Cannot coerce ARRAY types." );
        }

        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            throw new CoercionException( "Cannot coerce ARRAY types." );
        }
    }, List.class, XmlRpcConstants.ARRAY ),

    NIL( new ValueCoercion()
    {
        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            return null;
        }

        @Override
        public String toString( final Object value )
        {
            return null;
        }
    }, Void.class, "nil" ),

    INT( new ValueCoercion()
    {
        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            try
            {
                String val = value == null ? null : value.trim();
                return val == null || val.length() < 1 ? null : Integer.parseInt( val );
            }
            catch ( final NumberFormatException e )
            {
                throw new CoercionException( e.getMessage(), e );
            }
        }
    }, Integer.class, "int", "i4" ),

    BOOLEAN( new ValueCoercion()
    {
        @Override
        public String toString( final Object value )
            throws CoercionException
        {
            if ( value == null )
            {
                return null;
            }
            else
            {
                final String s = super.toString( value ).trim();
                if ( "1".equals( s ) || "0".equals( s ) )
                {
                    return s;
                }
                else
                {
                    return Boolean.valueOf( s ) ? "1" : "0";
                }
            }
        }

        @Override
        public Object fromString( final String value )
        {
            if ( value == null )
            {
                return null;
            }
            else
            {
                String val = value.trim();
                return "1".equals( val ) || Boolean.valueOf( val );
            }
        }
    }, Boolean.class, "boolean" ),

    STRING( new ValueCoercion()
    {
        @Override
        public Object fromString( final String value )
        {
            return value == null ? null : value.trim();
        }
    }, String.class, "string" ),

    DOUBLE( new ValueCoercion()
    {
        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            try
            {
                String val = value == null ? null : value.trim();
                return val == null || val.length() < 1 ? null : Double.parseDouble( val );
            }
            catch ( final NumberFormatException e )
            {
                throw new CoercionException( e.getMessage(), e );
            }
        }

        @Override
        public String toString( final Object value )
        {
            return value == null ? null : Double.toString( ( (Number) value ).doubleValue() );
        }
    }, Number.class, "double" ),

    DATETIME( new ValueCoercion()
    {
        private static final String FORMAT = "yyyyMMdd'T'HHmmss";

        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            try
            {
                String val = value == null ? null : value.trim();
                return val == null ? null : new SimpleDateFormat( FORMAT ).parse( value );
            }
            catch ( final ParseException e )
            {
                throw new CoercionException( "Cannot parse date: '" + value + "'.", e );
            }
        }

        @Override
        public String toString( final Object value )
            throws CoercionException
        {
            try
            {
                return value == null ? null : new SimpleDateFormat( FORMAT ).format( (Date) value );
            }
            catch ( final ClassCastException e )
            {
                throw new CoercionException( "Not a java.util.Date.", e );
            }
        }

    }, Date.class, "dateTime.iso8601" ),

    BASE64( new ValueCoercion()
    {
        @Override
        public Object fromString( final String value )
            throws CoercionException
        {
            if ( value == null )
            {
                return null;
            }

            final byte[] result = Base64.decodeBase64( value.trim() );
            if ( result.length < 1 && value.length() > 0 && !value.equals( "==" ) && !value.equals( "=" ) )
            {
                throw new CoercionException( "Invalid Base64 input: " + value );
            }

            return result;
        }

        @Override
        public String toString( final Object value )
            throws CoercionException
        {
            try
            {
                return value == null ? null
                                : new String(
                                              Base64.encodeBase64( value instanceof String ? ( (String) value ).getBytes()
                                                              : (byte[]) value ) );
            }
            catch ( final ClassCastException e )
            {
                throw new CoercionException( "Not a byte array.", e );
            }
        }

    }, byte[].class, "base64" ), ;

    private String[] tags;

    private Class<?> nativeType;

    private ValueCoercion coercion;

    private ValueType( final ValueCoercion coercion, final Class<?> nativeType, final String... tags )
    {
        this.coercion = coercion;
        this.nativeType = nativeType;
        this.tags = tags;
    }

    public ValueCoercion coercion()
    {
        return coercion;
    }

    public String getPrimaryTag()
    {
        return tags[0];
    }

    public static ValueType safeTypeFor( final Object value )
    {
        final ValueType type = typeFor( value );
        return type == null ? STRING : type;
    }

    public static ValueType typeFor( final Object value )
    {
        ValueType result = null;

        if ( value == null )
        {
            result = NIL;
        }
        else
        {
            final Class<?> cls = value instanceof Class<?> ? (Class<?>) value : value.getClass();
            for ( final ValueType vt : values() )
            {
                if ( vt.nativeType.isAssignableFrom( cls ) )
                {
                    result = vt;
                    break;
                }
            }
        }

        return result;
    }

    public static ValueType typeOf( final String tag )
    {
        if ( tag == null || tag.trim().length() < 1 )
        {
            return STRING;
        }

        for ( final ValueType type : values() )
        {
            for ( final String t : type.tags )
            {
                if ( t.equals( tag ) )
                {
                    return type;
                }
            }
        }

        return STRING;
    }
}
