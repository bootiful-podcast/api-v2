package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.integration.database.User;
import fm.bootifulpodcast.integration.database.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

// todo go back in history and restore the SecurityConfiguration that used to be here
// todo also go back into the pom.xml and restore the jwt-spring-boot-starter

@Configuration
@Slf4j
class CorsConfig {

	@Slf4j
	@Configuration
	public static class MyWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.info("launching " + MyWebSecurityConfigurerAdapter.class.getName());
			http //
					.authorizeRequests(ae -> ae //
							.mvcMatchers("/podcasts/search").authenticated() //
							.mvcMatchers("/podcasts/index").authenticated() //
							.mvcMatchers(HttpMethod.POST, "/podcasts/**").authenticated() //
							.mvcMatchers("/podcasts/*/profile-photo").permitAll() //
							.mvcMatchers("/podcasts/*/produced-audio").permitAll() //
							.mvcMatchers("/podcasts").authenticated() //
							.mvcMatchers("/actuator/health").permitAll() //
							.mvcMatchers("/actuator/health/**").permitAll() //
							.mvcMatchers("/site/podcasts").permitAll() //
							.mvcMatchers("/admin/**").authenticated() //
							.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()//
							.anyRequest().permitAll() //
					) //
					.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)//
					.cors(Customizer.withDefaults())//
					.csrf(AbstractHttpConfigurer::disable);
		}

	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(
				List.of("http://localhost:8080", "https://studio.bootifulpodcast.fm", "https://bootifulpodcast.fm"));
		configuration.setAllowedMethods(this.methods);
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(List.of("Authorization", "Requestor-Type"));
		configuration.setExposedHeaders(List.of("X-Get-Header"));
		configuration.setMaxAge(3600L);
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private final List<String> methods = List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD");

	@Bean
	UserDetailsService jdbcUserDetailsService(UserRepository repository) {
		return new JdbcUserDetailsService(repository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	WebMvcConfigurer corsConfigurer() {

		return new WebMvcConfigurer() {

			@Override
			public void addCorsMappings(CorsRegistry registry) {
				log.info("enabling global CORS supports");
				registry.addMapping("/**")//
						.allowedMethods(methods.toArray(new String[0]))//
						.allowedOriginPatterns("*")
						.allowedOrigins("https://studio.bootifulpodcast.fm", "https://bootifulpodcast.fm");
			}
		};
	}

}

@Slf4j
@RequiredArgsConstructor
class JdbcUserDetailsService implements UserDetailsService {

	@RequiredArgsConstructor
	private static class JpaUserDetails implements UserDetails {

		private final User user;

		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
			return Collections.singleton(new SimpleGrantedAuthority("USER"));
		}

		@Override
		public String getPassword() {
			return this.user.getPassword();
		}

		@Override
		public String getUsername() {
			return this.user.getUsername();
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

	}

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		log.info("looking for " + username + '.');
		var byUsername = this.userRepository.findByUsernameIgnoreCase((username + "").trim().toLowerCase()).stream()
				.map(JpaUserDetails::new).toList();

		if (byUsername.size() != 1) {
			throw new UsernameNotFoundException(
					"couldn't find one and only one instance of the user '" + username + "' ");
		}

		var result = byUsername.get(0);
		log.info("found  " + result.getUsername() + " with password [" + result.getPassword() + "]");
		return result;
	}

}