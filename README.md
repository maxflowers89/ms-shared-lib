##**RTS-CommonConfigLib**

Questa libreria contiene pricipalmente i bean e le relative configurazioni che solitamente vengono utilizzate nei microservizi illimity e il sistema di logging. Segue approfondimento package per package.

1. annotations
   - LogAroundThis: annotazione da mettere a livello di metodo per attivare il sistema di logging
2. callservices
   - CallsService: interfaccia che implementa un metodo di default setAuth(String auth) che va a costruire l'oggetto HttpHeaders popolandolo con gli headers ContentType a cui viene assegnato il valore application/json e Authorization popolato con la stringa che viene passata come parametro al metodo
     ```java

      public class ConsentsOrchestrator implements CallsService {

      public void saveConsents(String jwt, SaveConsents saveConsents, String locale) {
        restTemplate
          .postForEntity(
              HttpUtils.builder()
                  .gateway(consentsOrchestratorProperties.getDomain())
                  .uri(consentsOrchestratorProperties.getConsentsApi())
                  .queryParams(new LinkedMultiValueMap<>(Map
                      .of("locale", Collections.singletonList(locale))))
                  .build()
                  .createStringUrl(),
              new HttpEntity<>(saveConsents, setAuth(jwt)),  // in questo modo httpEntity viene arricchita con gli headers indicati
              Void.class);
        }

     }

     ```
3. configs
   - *async*, contiene la configurazione base di un bean di tipo ThreadPoolTaskExecutor che viene creato se e solo se nell'application.yml (o application.properties) del microservizio viene aggiunta la property **async.enabled** con valore true. Il bean viene instanziato con i seguenti valori di default, che ovviamente possono essere sovrascritti dalle proprietà del ms:

     ``` yaml
     async.corePoolSize:16
     async.maxPoolSize:64
     async.queueCapacity:1024
     ```

     **Molto importante, su questo executor viene settato un decorator che si occupa di copiare il ThreadContext di log4j2 nel nuovo thread che viene staccato dal metodo asincrono, in modo tale da non perdere tutto quello che c'era nel ThreadContext del thread chiamante e avere integrità dei log su DataDog**

     Dato che la classe implementa AsyncConfigurer questo executor verrà utilizzato di default sui metodi annotati con @Async di Spring, mentre segue un esempio di come utilizzare la soluzione se si vogliono utilizzare i metodi statici di CompletableFuture
     ```java
     public class ConsentsServiceImpl implements ConsentsService {

      @Autowired
      private Executor taskExecutor;

      @Override
      public Consents get(List<String> sections,
                          Boolean mandatory,
                          boolean light,
                          String app,
                          String userId,
                          String locale,
                          String jwt) {

        CompletableFuture<Consents> consentsCF = CompletableFuture.supplyAsync(() ->
                consentsOrchestrator.findConsents(jwt, sections, mandatory, light, locale),
            taskExecutor);

        ...
      }
     }
     ```
   - *resttemplate*, questo package contiene le classi necessarie per instanziare un bean RestTemplate con tutti i parametri necessari ad una gestione ottimale del keep-alive, che se troppo ampio può causare problemi di connessione. I parametri e i relativi default configurati sono i seguenti:

     ``` yaml
     rest-template.http.millis-connection-timeout:30000
     rest-template.http.millis-socket-timeout:30000
     rest-template.http.millis-request-timeout:30000
     rest-template.http.max-total-connections:10
     rest-template.http.default-max-per-route:5
     rest-template.http.millis-keep-alive:2000
     ```
     All'interno del package ci sono altre due classi molto importanti:
     - CustomClientHttpRequestInterceptor, settato come interceptor del bean suddetto, che intercetta tutte le chiamate fatte tramite restTemplate e gli applica tutte le logiche di logging definite in questa lib, oltre ad inniettare l'header X-Tx-Trace-Id con valore recuperato da traceId (inniettato nel ThreadContext da jwtAuthLib), molto importante per legare tra di loro i logs su DD.
     - CustomRestTemplateCustomizer che implementa RestTemplateCustomizer, una soluzione per uniformare i log a livello di chiamate rest. In fase di start-up dell'applicazione vengono intercettati i bean di tipo RestTemplate definiti dal ms (quindi oltre a quello definito in questa lib) a cui viene aggiunto l'interceptor discusso in precedenza

     **Per utilizzare il bean basta fare @Autowired di un oggetto di tipo RestTemplate**

   - APOLogger, contiene i pointcuts utilizzati dal sistema di logging:
     - @Pointcut("execution(* com.illimity..*.*(..))")
     - @Pointcut("@within(org.springframework.web.bind.annotation.RestControllerAdvice))")
     - @Pointcut("@within(org.springframework.web.bind.annotation.ControllerAdvice))")
     - @Pointcut("@within(org.springframework.web.bind.annotation.RestController))")
     - @Pointcut("execution(@com.illimity.rts.commonconfiglib.annotations.LogAroundThis * *(..))")

     E gli advices definiti su questi pointcuts:
     - @Before("(illimityClassesMonitoring() && restControllerMonitoring()) || loggingAnnotation()") in cui viene inserito lo start time della request nel ThreadContext di log4j2
     - @Around("(illimityClassesMonitoring() && restControllerMonitoring()) || loggingAnnotation()") dove vengono inserite tutte le coppie key/value nel ThreadContext log4j2 sia a livello di controller Spring che di eventuale chiamata restful
     - @AfterThrowing(pointcut = "restControllerMonitoring() || loggingAnnotation()", throwing = "ex") dove vengono inserite le coppie key/value nel ThreadContex log4j2 relative all'eccezione gestita
     - @AfterReturning(pointcut = "controllerAdviceMonitoring() || restControllerAdviceMonitoring() || loggingAnnotation()", returning = "returnValue") dove vengono inserite nel ThreadContext log4j2 tutte quelle coppie key/value di ritorno

   - LoggingConfigInit: insieme al package *init* contiene l'init del sistema di logging, che per essere attivato sul ms è importante aggiungere nel main questa riga e le successive props nell'application properties
     ```java
     ApplicationInitHook.addInitHooks(application);
     ```
     ``` yaml
     logging:
        root: default è WARN (in Library su Azure, #{log_level_root}#)
        app: default è INFO (in Library su Azure, #{log_level_app}#)
        log4j2-layout: plain | json (default è json) (in Library su Azure, #{log4j2_layout}#)
        logger-name: root package del ms, molto importante per far sì che vengano effetivamente attivati i log scritti nel ms
        size: int che definisce la lunghezza massima che può avere il log
        param-black-list: lista di param names il cui valore sui log sia in request che in response che verrà sostituito con la stringa <hidden>. Hardcodate ci sono i seguenti parametri password e oldPasswords
     ```

   **Se logging.app = DEBUG, verranno aggiunte al ThreadContext log4j2 request e response a livello di controller e rest call**
   - SprinConfig contiene la gestione dei CORS, dove di default abbiamo questi valori
     ``` yaml
     cors.mapping:/**
     cors.allowed_methods:HEAD,GET,PUT,POST,DELETE,PATCH
     ```
   - WebSecurityConfig estende WebSecurityConfigurerAdapter è la classe che contiene le configurazioni a livello http. Ci sono dei path configurati con permessi permitAll() e sono tutti quelli definiti nella configmap in *jwt.auth.path.whitelist* più alcuni default:
     - /actuator/health
     - /swagger-ui.html
     - /swagger-ui/**
     - /v3/api-docs/**
4. handlers è @RestControllerAdvice, quindi contiene la gestione delle eccezioni lanciate dall'applicazione fino al @RestController e la costruzione dell'oggetto da restituire al chiamante. Necessario definire in configmap la property **spring.profiles.active: #{spring_profile}#**, il cui valore è definito all'interno della Library su Azure. 
In caso di eccezioni custom definite all'interno del ms è stata introdotta una classe astratta così definita
   ```java
   @Data
   @EqualsAndHashCode(callSuper = true)
   public class AbstractErrorException extends RuntimeException {
     private final transient IErrorCode errorCode;
     private final HttpStatus httpStatus;

     public AbstractErrorException(String message, HttpStatus httpStatus, IErrorCode errorCode) {
       super(message);
       this.httpStatus = httpStatus;
       this.errorCode = errorCode;
    }
   }
   ```
   che viene già gestita dall'exception handler. Particolare importanza ha l'interfaccia IErrorCode
   ```java
   public interface IErrorCode {
     String getDescription();
     String name();
   }
   ```
   in quanto description e name sono i parametri passati nella response che viene restituita al chiamante.
5. utilities contiene una serie di classi utils:
   - CommonLoggingUtils, LoggingUtils, RestLoggingUtils definiscono metodi per la costruzione del ThreadContext e il print dei log a livello di controller e chiamate rest con relative eccezioni
   - JsonUtils contiene dei metodi per marshall/unmarshall di json in pojo e viceversa
   - HttpUtils contiene metodi per la costruzione funzionale degli url, ad es:
     
     ``` java
     HttpUtils.builder()
      .gateway(contentProperties.getDomain())
      .uri(contentProperties.getApis().getConsents())
      .pathVariables(new String[]{app})
      .queryParams(new LinkedMultiValueMap<>(Map.of(
          "locale", Collections.singletonList(locale)
      )))
      .build()
      .createStringUrl()
     ```
   - StreamCustomUtils contiene il metodo
     - toSingleton(), un Collector che data lo Stream estrae l'unico elemento all'interno e lancia un IllegalStateException se la lista contiene più elementi
     - toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) è identico a toMap, ma crea una LinkedHashMap
     - distinctByKey(Function<? super T, ?> keyExtractor) metodo che estrae da uno stream oggetti distinti sulla base della funzione passata in ingresso, prendendo il primo elemento nel caso di duplicati
