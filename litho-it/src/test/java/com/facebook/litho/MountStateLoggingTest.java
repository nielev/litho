/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests that Mount events are only logged when tracing is enabled. */
@RunWith(ComponentsTestRunner.class)
public class MountStateLoggingTest {
  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = spy(new TestComponentsLogger());
    when(mComponentsLogger.newEvent(any(int.class))).thenCallRealMethod();
    when(mComponentsLogger.newPerformanceEvent(any(int.class))).thenCallRealMethod();

    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  @Test
  public void testLogWhenTracing() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                .build();
          }
        });

    List<String> mountedNames = new ArrayList<>();
    List<String> unmountedNames = new ArrayList<>();
    mountedNames.add("TestViewComponent");
    mountedNames.add("TestViewComponent");

    verifyLoggingAndResetLogger(2, 0, mountedNames, unmountedNames);
  }

  @Test
  public void testNoLogWhenTracingDisabled() {
    when(mComponentsLogger.isTracing(any(LogEvent.class))).thenReturn(false);

    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                .build();
          }
        });

    final LogEvent event = mComponentsLogger.newPerformanceEvent(EVENT_MOUNT);
    verify(mComponentsLogger, never()).log(eq(event));
  }

  private void verifyLoggingAndResetLogger(
      int mountedCount,
      int unmountedCount,
      List<String> mountedNames,
      List<String> unmountedNames) {
    final LogEvent event = mComponentsLogger.newPerformanceEvent(EVENT_MOUNT);
    event.addParam(PARAM_MOUNTED_COUNT, String.valueOf(mountedCount));
    event.addParam(PARAM_UNMOUNTED_COUNT, String.valueOf(unmountedCount));
    event.addJsonParam(PARAM_MOUNTED_CONTENT, mountedNames);
    event.addJsonParam(PARAM_UNMOUNTED_CONTENT, unmountedNames);

    verify(mComponentsLogger).log(eq(event));
    reset(mComponentsLogger);
  }
}
