package org.serdaroquai.me;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.serdaroquai.me.misc.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableAsync
@EnableScheduling
public class ApplicationConfig implements AsyncConfigurer{

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
		
	@PostConstruct
	public void init() {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	@Bean
	public PublicKey loadPublicKey(@Value("${publicKey}") String stored) throws GeneralSecurityException {
		return Util.loadPublicKey(stored);
	}
	
	@Bean
	public PrivateKey loadPrivateKey(@Value("${privateKey}") String stored) throws GeneralSecurityException {
		return Util.loadPrivateKey(stored);
	}
	
	//spring asynchronous event handling
	@Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster(Executor executor) {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        return eventMulticaster;
    }
	
	@Override
	@Bean("asyncExecutor")
	public Executor getAsyncExecutor() {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setDaemon(true);
		taskExecutor.setThreadNamePrefix("asnycExec");
		return taskExecutor;
	}
	
	@Bean
	@Primary
	public Executor getExecutor() {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setThreadNamePrefix("primeExec");
		return taskExecutor;
	}
    
	@Bean
	TelegramBot telegramBot(@Value("${telegram.token}") String token, 
			@Value("${telegram.botname}") String botname,
			@Value("${telegram.chatId}") String chatId,
			@Value("${telegram.isEnabled:true}") boolean enabled) throws TelegramApiRequestException {
		
		if (enabled) {
			ApiContextInitializer.init();			
		}
        return new TelegramBot(token, botname, chatId, enabled);

	}
	
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
	
	@Bean
	public RestTemplate restTemplate(
			@Value("${proxy.ip:}") String ip, 
			@Value("${proxy.port:}") String port,
			@Value("${restService.connectTimeout:15000}") int connectTimeout,
			@Value("${restService.readTimeout:15000}") int readTimeout) {
	    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
	    if (!"".equals(ip) && !"".equals(port)) {
	    	Proxy proxy= new Proxy(Type.HTTP, new InetSocketAddress(ip, Integer.valueOf(port)));
	    	requestFactory.setProxy(proxy);
	    }
	    requestFactory.setConnectTimeout(connectTimeout);
	    requestFactory.setReadTimeout(readTimeout);
	    return new RestTemplate(requestFactory);
	}
}