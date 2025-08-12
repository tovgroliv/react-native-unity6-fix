import React from 'react';

import NativeUnityView, { Commands } from './specs/UnityViewNativeComponent';
import type { DirectEventHandler } from 'react-native/Libraries/Types/CodegenTypes';
import { Platform } from 'react-native';
import type { HostComponent } from 'react-native';
import type { NativeProps } from './specs/UnityViewNativeComponent';

// Добавляем детальное логирование
const LOG_TAG = '[UnityView]';
const log = (message: string, ...args: any[]) => {
  console.log(`${LOG_TAG} ${message}`, ...args);
};

type UnityViewContentUpdateEvent = Readonly<{
  message: string;
}>;

type RNUnityViewProps = {
  androidKeepPlayerMounted?: boolean;
  fullScreen?: boolean;
  onUnityMessage?: DirectEventHandler<UnityViewContentUpdateEvent>;
  onPlayerUnload?: DirectEventHandler<UnityViewContentUpdateEvent>;
  onPlayerQuit?: DirectEventHandler<UnityViewContentUpdateEvent>;
};

type ComponentRef = React.ElementRef<HostComponent<NativeProps>>;

export default class UnityView extends React.Component<RNUnityViewProps> {
  ref = React.createRef<ComponentRef>();

  constructor(props: RNUnityViewProps) {
    super(props);
    log('Unity View constructor called');
  }

  public postMessage = (
    gameObject: string,
    methodName: string,
    message: string
  ) => {
    log('postMessage called:', { gameObject, methodName, message });
    if (this.ref.current) {
      Commands.postMessage(this.ref.current, gameObject, methodName, message);
    } else {
      log('ERROR: ref.current is null in postMessage');
    }
  };

  public unloadUnity = () => {
    log('unloadUnity called');
    if (this.ref.current) {
      Commands.unloadUnity(this.ref.current);
    } else {
      log('ERROR: ref.current is null in unloadUnity');
    }
  };

  public pauseUnity(pause: boolean) {
    log('pauseUnity called:', pause);
    if (this.ref.current) {
      Commands.pauseUnity(this.ref.current, pause);
    } else {
      log('ERROR: ref.current is null in pauseUnity');
    }
  }

  public resumeUnity() {
    log('resumeUnity called');
    if (this.ref.current) {
      Commands.resumeUnity(this.ref.current);
    } else {
      log('ERROR: ref.current is null in resumeUnity');
    }
  }

  public windowFocusChanged(hasFocus = true) {
    log('windowFocusChanged called:', hasFocus);
    if (Platform.OS !== 'android') return;

    if (this.ref.current) {
      Commands.windowFocusChanged(this.ref.current, hasFocus);
    } else {
      log('ERROR: ref.current is null in windowFocusChanged');
    }
  }

  private getProps() {
    const props = {
      ...this.props,
      onUnityMessage: (event: any) => {
        log('Unity message received:', event.nativeEvent);
        this.props.onUnityMessage?.(event);
      },
      onPlayerUnload: (event: any) => {
        log('Unity player unloaded:', event?.nativeEvent);
        this.props.onPlayerUnload?.(event);
      },
      onPlayerQuit: (event: any) => {
        log('Unity player quit:', event?.nativeEvent);
        this.props.onPlayerQuit?.(event);
      }
    };
    return props;
  }

  componentDidMount() {
    log('Unity View mounted');
  }

  componentWillUnmount() {
    log('Unity View will unmount');
    if (this.ref.current) {
      Commands.unloadUnity(this.ref.current);
    }
  }

  render(): React.ReactElement {
    log('Unity View render called');
    return <NativeUnityView ref={this.ref} {...this.getProps()} />;
  }
}
