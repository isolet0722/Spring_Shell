package com.ksj.springshell;

import java.util.Arrays;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.Shell;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksj.springshell.jpa.TargetEntity;
import com.ksj.springshell.jpa.TargetRepository;

@ShellComponent
@ShellCommandGroup("Main Commands")
public class Commands {
		
	@Autowired
	private Shell shell;
	
	@Autowired
	private ShellHelper shellHelper;
	
	@Autowired
	private TargetRepository targetRepository;
	
	private TargetEntity targetEntity = TargetEntity.builder().build();
		
	// echo : 입력한 내용을 콘솔에 출력
	@ShellMethod("Prints what has been entered")
	public String echo(String msg) {
		return shellHelper.getInfoMessage(msg);
	}
	
	// target : 이후의 작업들에서 대상이 될 애플리케이션의 IP, PORT를 설정하고 DB에 저장함. IP는 필수값이며, PORT는 입력하지 않으면 기본값인 80으로 설정됨.
	@ShellMethod(key = { "target", "connect" }, value = "Set connection target's IP address and port")
	public String target(String ip, @ShellOption(defaultValue = "80") String port) {
		if(ip.matches("^(?:(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])(\\.(?!$)|$)){4}$") 
				|| port.matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$")) {
			targetEntity.setId(1L);
			targetEntity.setIp(ip);
			targetEntity.setPort(port);
			targetRepository.saveAndFlush(targetEntity);
		} else {
			return shellHelper.getErrorMessage("[ Invaild IP address or Port. ]");
		}
		
		return shellHelper.getSuccessMessage("[ Ready to Connect " + targetRepository.findById(1L).get().getIp() + ":" + targetRepository.findById(1L).get().getPort() + " ]");
	}
	
	// 타겟 애플리케이션의 특정 쉘 스크립트를 실행하고 성공/실패 여부를 표시. DB에 타겟 정보가 없다면 기본값으로 설정됨.
	@ShellMethod("Execute Shell script from the target")
	@ShellMethodAvailability("targetCheck")
	public String hello(@ShellOption(defaultValue = "") String args) {
		if(!targetRepository.existsById(1L)) setDefaultAddress();
		
		String url = "http://" + targetRepository.findById(1L).get().getIp() + ":" + targetRepository.findById(1L).get().getPort() + "/hello";
		
		System.out.println(shellHelper.getInfoMessage("[ Connecting to " + url + " ]"));
		
		RestTemplate rt = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		String[] params = Arrays.stream(args.split(",")).map(String::trim).toArray(String[]::new);
		for(int i = 0; i < params.length; i++) map.add("args", params[i] + " ");
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response = rt.postForEntity(url, request, String.class);
		
		return shellHelper.getSuccessMessage(response.getBody());
	}
	
	// monitor : 시스템 모니터링 정보를 확인하는 쉘 스크립트(monitor.sh) 실행 결과를 JSON 포맷으로 받아옴. 
	@ShellMethod("It receives monitoring data(Shell Script) from target server")
	public String monitor() throws Exception {
		if(!targetRepository.existsById(1L)) setDefaultAddress();
		
		String url = "http://" + targetRepository.findById(1L).get().getIp() + ":" + targetRepository.findById(1L).get().getPort() + "/monitor";

		System.out.println(shellHelper.getInfoMessage("[ Connecting to " + url + " ]"));

		RestTemplate rt = new RestTemplate();
		String response = rt.getForObject(url, String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(response, Object.class));
		
		return shellHelper.getSuccessMessage(indented);
	}

	// sa : Spring Actuator가 제공하는 애플리케이션 모니터링 정보를 JSON 포맷으로 받아옴.
	@ShellMethod(key = { "sa", "actuator" }, value = "It receives monitoring data(Spring Actuator) from target server")
	public String sa(String info, @ShellOption(defaultValue = "") String option) {
		String url = "http://" + targetRepository.findById(1L).get().getIp() + ":" + targetRepository.findById(1L).get().getPort() + "/" + info + "/" + option;
		
		RestTemplate rt = new RestTemplate();
		String response = rt.getForObject(url, String.class);
		
		return shellHelper.getInfoMessage(response);
	}
	
	// hcheck : 타겟 애플리케이션의 헬스 체크 기능을 토글. 인자로 IP와 PORT를 전달할 경우, 해당 주소 정보로 타겟의 헬스 체크 대상을 변경함.
	@ShellMethod(key = { "hcheck", "healthcheck" }, value = "Toggle the health check function of the target server")
	public String hcheck(@ShellOption(defaultValue = "") String ip, @ShellOption(defaultValue = "") String port) {
		if(!ip.isEmpty() || !port.isEmpty()) {
			if(!ip.matches("^(?:(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])(\\.(?!$)|$)){4}$")
					|| !port.matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$")) {
				return shellHelper.getErrorMessage("[ Invaild IP address or Port. ]");
			}
		}
		
		String url = "http://" + targetRepository.findById(1L).get().getIp() + ":" + targetRepository.findById(1L).get().getPort() + "/healthcheck";
		
		RestTemplate rt = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("ip", ip);
		map.add("port", port);
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response = rt.postForEntity(url, request, String.class);
		
		return shellHelper.getInfoMessage(response.getBody());
	}
	
	@ShellMethod("Show Command List")
	public void ls() {
		Iterator<String> it = shell.listCommands().keySet().iterator();
		while(it.hasNext()) System.out.println(shellHelper.getInfoMessage(it.next()));
	}
	
	private void setDefaultAddress() {
		targetEntity.setId(1L);
		targetEntity.setIp("127.0.0.1");
		targetEntity.setPort("80");
		targetRepository.saveAndFlush(targetEntity);
	}

}
