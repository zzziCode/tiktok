package com.zzzi.common.utils;

import io.jsonwebtoken.*;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @author zzzi
 * @date 2024/3/25 18:56
 * 根据用户名和用户id生成用户的token
 */
public class JwtUtils {
    //过期时间 ms (1 day)
    private static long tokenExpiration = 24 * 60 * 60 * 1000;
    //签名密钥
    private static String tokenSignKey = "9q8w5sad65sca54fas65";

    //生成token

    /**
     * @author zzzi
     * @date 2024/3/25 12:58
     * 只要用户id和用户名一致，前后的token就是一致的
     */
    public static String createToken(Long userId, String userName) {
        String token = Jwts.builder()
                .setSubject("DY-USER")
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .claim("userId", userId)
                .claim("userName", userName)
                .signWith(SignatureAlgorithm.HS512, tokenSignKey)
                .compressWith(CompressionCodecs.GZIP)
                .compact();
        return token;
    }

    //根据token字符串得到用户id
    public static Long getUserIdByToken(String token) {
        if (StringUtils.isEmpty(token)) return null;
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        Long userId = (Long) claims.get("userId");
        return userId.longValue();
    }

    //根据token字符串得到用户名称
    public static String getUserNameByToken(String token) {
        if (StringUtils.isEmpty(token)) return "";
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        return (String) claims.get("userName");
    }
}
