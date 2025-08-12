#import "RNUnityView.h"
#ifdef DEBUG
#include <mach-o/ldsyms.h>
#endif
#ifdef RCT_NEW_ARCH_ENABLED
using namespace facebook::react;
#endif

// Добавляем детальное логирование
#define UNITY_LOG(fmt, ...) NSLog(@"[RNUnityView] " fmt, ##__VA_ARGS__)

NSString *bundlePathStr = @"/Frameworks/UnityFramework.framework";
int gArgc = 1;

UnityFramework* UnityFrameworkLoad() {
    UNITY_LOG(@"Starting UnityFrameworkLoad");
    
    NSString* bundlePath = nil;
    bundlePath = [[NSBundle mainBundle] bundlePath];
    bundlePath = [bundlePath stringByAppendingString: bundlePathStr];
    
    UNITY_LOG(@"Unity bundle path: %@", bundlePath);
    
    // Проверяем существование файла
    NSFileManager *fileManager = [NSFileManager defaultManager];
    if (![fileManager fileExistsAtPath:bundlePath]) {
        UNITY_LOG(@"ERROR: Unity framework does not exist at path: %@", bundlePath);
        
        // Попробуем найти UnityFramework в других местах
        NSArray *possiblePaths = @[
            @"/Frameworks/UnityFramework.framework",
            @"/UnityFramework.framework", 
            @"/../Frameworks/UnityFramework.framework"
        ];
        
        for (NSString *possiblePath in possiblePaths) {
            NSString *testPath = [[[NSBundle mainBundle] bundlePath] stringByAppendingString:possiblePath];
            UNITY_LOG(@"Checking alternative path: %@", testPath);
            if ([fileManager fileExistsAtPath:testPath]) {
                UNITY_LOG(@"Found Unity framework at alternative path: %@", testPath);
                bundlePath = testPath;
                break;
            }
        }
        
        // Если все равно не найден
        if (![fileManager fileExistsAtPath:bundlePath]) {
            UNITY_LOG(@"ERROR: Unity framework not found in any expected location");
            
            // Показываем содержимое главного бандла
            NSString *mainBundlePath = [[NSBundle mainBundle] bundlePath];
            NSArray *contents = [fileManager contentsOfDirectoryAtPath:mainBundlePath error:nil];
            UNITY_LOG(@"Main bundle contents: %@", contents);
            
            // Показываем содержимое папки Frameworks если она есть
            NSString *frameworksPath = [mainBundlePath stringByAppendingString:@"/Frameworks"];
            if ([fileManager fileExistsAtPath:frameworksPath]) {
                NSArray *frameworkContents = [fileManager contentsOfDirectoryAtPath:frameworksPath error:nil];
                UNITY_LOG(@"Frameworks folder contents: %@", frameworkContents);
            } else {
                UNITY_LOG(@"Frameworks folder does not exist at: %@", frameworksPath);
            }
            
            return nil;
        }
    }

    NSBundle* bundle = [NSBundle bundleWithPath: bundlePath];
    if (!bundle) {
        UNITY_LOG(@"ERROR: Could not create bundle with path: %@", bundlePath);
        return nil;
    }
    
    UNITY_LOG(@"Bundle created successfully, isLoaded: %@", [bundle isLoaded] ? @"YES" : @"NO");
    
    if ([bundle isLoaded] == false) {
        BOOL loadResult = [bundle load];
        UNITY_LOG(@"Bundle load result: %@", loadResult ? @"SUCCESS" : @"FAILED");
        if (!loadResult) {
            UNITY_LOG(@"ERROR: Failed to load Unity bundle");
            return nil;
        }
    }

    Class principalClass = [bundle principalClass];
    UNITY_LOG(@"Principal class: %@", principalClass);
    
    if (!principalClass) {
        UNITY_LOG(@"ERROR: Principal class is nil");
        return nil;
    }

    UnityFramework* ufw = [principalClass getInstance];
    if (!ufw) {
        UNITY_LOG(@"ERROR: Could not get UnityFramework instance");
        return nil;
    }
    
    UNITY_LOG(@"UnityFramework instance created, appController exists: %@", [ufw appController] ? @"YES" : @"NO");
    
    if (![ufw appController])
    {
#ifdef DEBUG
      [ufw setExecuteHeader: &_mh_dylib_header];
      UNITY_LOG(@"Set execute header for DEBUG");
#else
      [ufw setExecuteHeader: &_mh_execute_header];
      UNITY_LOG(@"Set execute header for RELEASE");
#endif
    }

    [ufw setDataBundleId: [bundle.bundleIdentifier cStringUsingEncoding:NSUTF8StringEncoding]];
    UNITY_LOG(@"Set data bundle ID: %@", bundle.bundleIdentifier);

    return ufw;
}

@implementation RNUnityView

NSDictionary* appLaunchOpts;

static RNUnityView *sharedInstance;

- (bool)unityIsInitialized {
    bool isInitialized = [self ufw] && [[self ufw] appController];
    UNITY_LOG(@"Unity is initialized: %@", isInitialized ? @"YES" : @"NO");
    return isInitialized;
}

- (void)initUnityModule {
    UNITY_LOG(@"initUnityModule called");
    
    @try {
        if([self unityIsInitialized]) {
            UNITY_LOG(@"Unity already initialized, returning");
            return;
        }

        UNITY_LOG(@"Loading Unity framework...");
        [self setUfw: UnityFrameworkLoad()];
        
        if (![self ufw]) {
            UNITY_LOG(@"ERROR: Failed to load Unity framework");
            return;
        }
        
        UNITY_LOG(@"Registering framework listener...");
        [[self ufw] registerFrameworkListener: self];

        unsigned count = (int) [[[NSProcessInfo processInfo] arguments] count];
        char **array = (char **)malloc((count + 1) * sizeof(char*));

        for (unsigned i = 0; i < count; i++)
        {
             array[i] = strdup([[[[NSProcessInfo processInfo] arguments] objectAtIndex:i] UTF8String]);
        }
        array[count] = NULL;

        UNITY_LOG(@"Running Unity embedded with argc: %d", gArgc);
        [[self ufw] runEmbeddedWithArgc: gArgc argv: array appLaunchOpts: appLaunchOpts];
        
        UNITY_LOG(@"Setting quit handler...");
        [[self ufw] appController].quitHandler = ^(){ 
            UNITY_LOG(@"Unity quit handler called"); 
        };
        
        UNITY_LOG(@"Removing Unity root view from its superview...");
        [self.ufw.appController.rootView removeFromSuperview];

        if (@available(iOS 13.0, *)) {
            UNITY_LOG(@"Setting window scene to nil (iOS 13+)");
            [[[[self ufw] appController] window] setWindowScene: nil];
        } else {
            UNITY_LOG(@"Setting screen to nil (iOS < 13)");
            [[[[self ufw] appController] window] setScreen: nil];
        }

        UNITY_LOG(@"Adding Unity root view as subview...");
        [[[[self ufw] appController] window] addSubview: self.ufw.appController.rootView];
        [[[[self ufw] appController] window] makeKeyAndVisible];
        [[[[[[self ufw] appController] window] rootViewController] view] setNeedsLayout];

        UNITY_LOG(@"Registering API for native calls...");
        [NSClassFromString(@"FrameworkLibAPI") registerAPIforNativeCalls:self];
        
        UNITY_LOG(@"Unity initialization completed successfully");
    }
    @catch (NSException *e) {
        UNITY_LOG(@"ERROR during Unity initialization: %@", e);
        UNITY_LOG(@"Exception reason: %@", e.reason);
        UNITY_LOG(@"Exception callStackSymbols: %@", e.callStackSymbols);
    }
}

- (void)layoutSubviews {
   UNITY_LOG(@"layoutSubviews called, bounds: %@", NSStringFromCGRect(self.bounds));
   [super layoutSubviews];

   if([self unityIsInitialized]) {
      UNITY_LOG(@"Unity is initialized, setting root view frame and adding as subview");
      self.ufw.appController.rootView.frame = self.bounds;
      [self addSubview:self.ufw.appController.rootView];
      UNITY_LOG(@"Unity root view added with frame: %@", NSStringFromCGRect(self.ufw.appController.rootView.frame));
   } else {
      UNITY_LOG(@"Unity is NOT initialized during layoutSubviews");
   }
}

- (void)pauseUnity:(BOOL * _Nonnull)pause {
    if([self unityIsInitialized]) {
        [[self ufw] pause:pause];
    }
}

- (void)unloadUnity {
    UIWindow * main = [[[UIApplication sharedApplication] delegate] window];
    if(main != nil) {
        [main makeKeyAndVisible];

        if([self unityIsInitialized]) {
            [[self ufw] unloadApplication];
        }
    }
}

- (void)sendMessageToMobileApp:(NSString *)message {
    if (self.onUnityMessage) {
        NSDictionary* data = @{
            @"message": message
        };

        self.onUnityMessage(data);
    }
}

- (void)unityDidUnload:(NSNotification*)notification {
    if([self unityIsInitialized]) {
        [[self ufw] unregisterFrameworkListener:self];
        [self setUfw: nil];

        if (self.onPlayerUnload) {
            self.onPlayerUnload(nil);
        }
    }
}

- (void)unityDidQuit:(NSNotification*)notification {
    if([self unityIsInitialized]) {
        [[self ufw] unregisterFrameworkListener:self];
        [self setUfw: nil];

        if (self.onPlayerQuit) {
            self.onPlayerQuit(nil);
        }
    }
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onUnityMessage", @"onPlayerUnload", @"onPlayerQuit"];
}

- (void)postMessage:(NSString *)gameObject methodName:(NSString*)methodName message:(NSString*) message {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[self ufw] sendMessageToGOWithName:[gameObject UTF8String] functionName:[methodName UTF8String] message:[message UTF8String]];
    });
}

#ifdef RCT_NEW_ARCH_ENABLED
- (void)prepareForRecycle {
    [super prepareForRecycle];

    if ([self unityIsInitialized]) {
      [[self ufw] unloadApplication];

      NSArray *viewsToRemove = self.subviews;
      for (UIView *v in viewsToRemove) {
          [v removeFromSuperview];
      }

      [self setUfw:nil];
    }
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<RNUnityViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
  UNITY_LOG(@"initWithFrame called (New Architecture), frame: %@", NSStringFromCGRect(frame));
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RNUnityViewProps>();
    _props = defaultProps;

    self.onUnityMessage = [self](NSDictionary* data) {
      UNITY_LOG(@"Unity message received: %@", data);
      if (_eventEmitter != nil) {
        auto gridViewEventEmitter = std::static_pointer_cast<RNUnityViewEventEmitter const>(_eventEmitter);
        facebook::react::RNUnityViewEventEmitter::OnUnityMessage event = {
          .message=[[data valueForKey:@"message"] UTF8String]
        };
        gridViewEventEmitter->onUnityMessage(event);
      }
    };
  }

  return self;
}

- (void)updateEventEmitter:(EventEmitter::Shared const &)eventEmitter {
    [super updateEventEmitter:eventEmitter];
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps {
    UNITY_LOG(@"updateProps called (New Architecture)");
    if (![self unityIsInitialized]) {
      UNITY_LOG(@"Unity not initialized, calling initUnityModule");
      [self initUnityModule];
    } else {
      UNITY_LOG(@"Unity already initialized");
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(nonnull const NSString *)commandName args:(nonnull const NSArray *)args {
    RCTRNUnityViewHandleCommand(self, commandName, args);
}

Class<RCTComponentViewProtocol> RNUnityViewCls(void) {
    return RNUnityView.class;
}

#else

-(id)initWithFrame:(CGRect)frame {
    UNITY_LOG(@"initWithFrame called (Old Architecture), frame: %@", NSStringFromCGRect(frame));
    self = [super initWithFrame:frame];

    if (self) {
        UNITY_LOG(@"Calling initUnityModule from Old Architecture init");
        [self initUnityModule];
    }

    return self;
}

#endif

@end
