#import "RNUnityViewDebug.h"

#define DEBUG_LOG(fmt, ...) NSLog(@"[RNUnityViewDebug] " fmt, ##__VA_ARGS__)

@implementation RNUnityViewDebug

+ (void)load {
    DEBUG_LOG(@"=== RNUnityViewDebug +load called ===");
    [self checkArchitecture];
    [self checkUnityFramework];
}

+ (void)checkArchitecture {
    DEBUG_LOG(@"=== Checking React Native Architecture ===");
    
#ifdef RCT_NEW_ARCH_ENABLED
    DEBUG_LOG(@"NEW ARCHITECTURE (Fabric) is ENABLED");
#else
    DEBUG_LOG(@"OLD ARCHITECTURE (Paper) is being used");
#endif
    
    // Проверяем, зарегистрирован ли компонент
    Class managerClass = NSClassFromString(@"RNUnityViewManager");
    if (managerClass) {
        DEBUG_LOG(@"RNUnityViewManager class found: %@", managerClass);
    } else {
        DEBUG_LOG(@"ERROR: RNUnityViewManager class NOT found");
    }
    
    Class viewClass = NSClassFromString(@"RNUnityView");
    if (viewClass) {
        DEBUG_LOG(@"RNUnityView class found: %@", viewClass);
    } else {
        DEBUG_LOG(@"ERROR: RNUnityView class NOT found");
    }
}

+ (void)checkUnityFramework {
    DEBUG_LOG(@"=== Checking Unity Framework ===");
    
    NSString *bundlePathStr = @"/Frameworks/UnityFramework.framework";
    NSString* bundlePath = [[[NSBundle mainBundle] bundlePath] stringByAppendingString: bundlePathStr];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL exists = [fileManager fileExistsAtPath:bundlePath];
    DEBUG_LOG(@"Unity framework exists at %@: %@", bundlePath, exists ? @"YES" : @"NO");
    
    if (!exists) {
        DEBUG_LOG(@"Searching for Unity framework in alternative locations...");
        
        NSString *mainBundlePath = [[NSBundle mainBundle] bundlePath];
        NSArray *contents = [fileManager contentsOfDirectoryAtPath:mainBundlePath error:nil];
        DEBUG_LOG(@"Main bundle contents: %@", contents);
        
        NSString *frameworksPath = [mainBundlePath stringByAppendingString:@"/Frameworks"];
        if ([fileManager fileExistsAtPath:frameworksPath]) {
            NSArray *frameworkContents = [fileManager contentsOfDirectoryAtPath:frameworksPath error:nil];
            DEBUG_LOG(@"Frameworks folder contents: %@", frameworkContents);
        } else {
            DEBUG_LOG(@"Frameworks folder does not exist");
        }
    }
    
    // Проверяем, можем ли мы найти Unity классы
    Class unityFrameworkClass = NSClassFromString(@"UnityFramework");
    if (unityFrameworkClass) {
        DEBUG_LOG(@"UnityFramework class found: %@", unityFrameworkClass);
    } else {
        DEBUG_LOG(@"ERROR: UnityFramework class NOT found - Unity framework not properly linked");
    }
}

@end
