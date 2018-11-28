//package celtech.roboxbase.billing.killbill;
//
//
//import org.killbill.billing.catalog.api.Currency;
//import org.killbill.billing.client.KillBillClientException;
//import org.killbill.billing.client.KillBillHttpClient;
//import org.killbill.billing.client.RequestOptions;
//import org.killbill.billing.client.api.gen.AccountApi;
//import org.killbill.billing.client.model.gen.Account;
//
///**
// *
// * @author George Salter
// */
//public class KillbillAccountService {
//    
//    private static final String USERNAME = "admin";
//    private static final String PASSWORD = "password";
//    private static final String API_KEY = "robox";
//    private static final String API_SECRET = "aligator";
//    
//    private KillBillHttpClient killBillHttpClient = new KillBillHttpClient(String.format("http://%s:%d", "192.168.1.117", 8080),
//                                                               USERNAME,
//                                                               PASSWORD,
//                                                               API_KEY,
//                                                               API_SECRET,
//                                                               null,
//                                                               null,
//                                                               1000,
//                                                               5000,
//                                                               5000);
//    
//    private AccountApi accountApi = new AccountApi(killBillHttpClient);
//    
//    private String createdBy = "me";
//    private String reason = "Going through my first tutorial";
//    private String comment = "I like it!";
//    
//    RequestOptions requestOptions = RequestOptions.builder()
//            .withCreatedBy(createdBy)
//            .withReason(reason)
//            .withComment(comment)
//            .withTenantApiKey(API_KEY)
//            .withTenantApiSecret(API_SECRET)
//            .build();
//    
//    public Account createAccount(String name, String email) throws KillBillClientException {
//        Account body = new Account();
//        body.setName(name);
//        body.setEmail(email);
//        body.setCurrency(Currency.GBP);
//        Account result = accountApi.createAccount(body, requestOptions);
//        return result;
//    }
//}
