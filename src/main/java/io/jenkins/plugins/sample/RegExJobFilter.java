package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.views.ViewJobFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExJobFilter extends AbstractIncludeExcludeJobFilter {

    public enum ValueType {
        NAME {
            @Override
            void doGetMatchValues(TopLevelItem item, Options options, List<String> values) {
                if (options.matchName) {
                    values.add(item.getName());
                }
                if (options.matchFullName) {
                    values.add(item.getFullName());
                }
                if (options.matchDisplayName) {
                    values.add(item.getDisplayName());
                }
                if (options.matchFullDisplayName) {
                    values.add(item.getFullDisplayName());
                }
            }
        },
        FOLDER_NAME {
            @Override
            void doGetMatchValues(TopLevelItem item, Options options, List<String> values) {
                if (item.getParent() != null) {
                    if (options.matchName && item.getParent() instanceof Item) {
                        values.add(((Item)item.getParent()).getName());
                    }
                    if (options.matchFullName) {
                        values.add(item.getParent().getFullName());
                    }
                    if (options.matchDisplayName) {
                        values.add(item.getParent().getDisplayName());
                    }
                    if (options.matchFullDisplayName) {
                        values.add(item.getParent().getFullDisplayName());
                    }
                }
            }
        },
        BUILD_VERSION {
            @Override
            void doGetMatchValues(TopLevelItem item, Options options, List<String> values) {
                if (item.getAllJobs() != null) {
                    ArrayList<Job> jobs = new ArrayList<>(item.getAllJobs());
                    for (Job job : jobs) {
                        for (Iterator iterator = job.getBuilds().listIterator(); iterator.hasNext(); ) {
                            Run run = (Run) iterator.next();
                            if (options.matchFullName || options.matchFullDisplayName) {
                                values.add(run.getFullDisplayName());
                            }
                            if (options.matchName || options.matchDisplayName) {
                                values.add(run.getDisplayName());
                            }
                        }
                    }
                }
            }
        };

        abstract void doGetMatchValues(TopLevelItem item, Options options, List<String> values);

        public List<String> getMatchValues(TopLevelItem item, Options options) {
            List<String> values = new ArrayList<String>();
            doGetMatchValues(item, options, values);
            return values;
        }
    }

    public static class Options {
        public final boolean matchName;
        public final boolean matchFullName;
        public final boolean matchDisplayName;
        public final boolean matchFullDisplayName;

        public Options(boolean matchName, boolean matchFullName, boolean matchDisplayName, boolean matchFullDisplayName) {
            this.matchName = matchName;
            this.matchFullName = matchFullName;
            this.matchDisplayName = matchDisplayName;
            this.matchFullDisplayName = matchFullDisplayName;
        }
    }

    transient private ValueType valueType;
    private String valueTypeString;
    private String regex;
    transient private Pattern pattern;
    private boolean matchName;
    private boolean matchFullName;
    private boolean matchDisplayName;
    private boolean matchFullDisplayName;

    public RegExJobFilter(String regex, String includeExcludeTypeString, String valueTypeString) {
        this(regex, includeExcludeTypeString, valueTypeString, true, false, false, false);
    }

    @DataBoundConstructor
    public RegExJobFilter(String regex, String includeExcludeTypeString, String valueTypeString,
                          boolean matchName, boolean matchFullName, boolean matchDisplayName, boolean matchFullDisplayName) {
        super(includeExcludeTypeString);
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.valueTypeString = valueTypeString;
        this.valueType = ValueType.valueOf(valueTypeString);
        this.matchName = matchName;
        this.matchFullName = matchFullName;
        this.matchDisplayName = matchDisplayName;
        this.matchFullDisplayName = matchFullDisplayName;
        initOptions();
    }

    private void initOptions() {
        if (!this.matchName && !this.matchFullName && !this.matchDisplayName && !this.matchFullDisplayName) {
            this.matchName = true;
        }
    }

    Object readResolve() {
        if (regex != null) {
            pattern = Pattern.compile(regex);
        }
        if (valueTypeString != null) {
            valueType = ValueType.valueOf(valueTypeString);
        }
        initOptions();
        return super.readResolve();
    }

    public boolean matches(TopLevelItem item) {
        List<String> matchValues = valueType.getMatchValues(item, getOptions());
        for (String matchValue: matchValues) {
            if (matchValue != null &&
                    pattern.matcher(matchValue).matches()) {
                return true;
            }
        }
        return false;
    }

    public String getValueTypeString() {
        return valueTypeString;
    }

    public String getRegex() {
        return regex;
    }

    public boolean isMatchFullName() {
        return matchFullName;
    }

    public boolean isMatchName() {
        return matchName;
    }

    public boolean isMatchDisplayName() {
        return matchDisplayName;
    }
    public boolean isMatchFullDisplayName() {
        return matchFullDisplayName;
    }

    public Options getOptions() {
        return new Options(matchName, matchFullName, matchDisplayName, matchFullDisplayName);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return io.jenkins.plugins.sample.filters.Messages.RegExJobFilter_DisplayName();
        }

        public FormValidation doCheckRegex(@QueryParameter String value ) throws IOException, ServletException, InterruptedException  {
            String v = Util.fixEmpty(value);
            if (v != null) {
                try {
                    Pattern.compile(v);
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error(pse.getMessage());
                }
            }
            return FormValidation.ok();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/regex-view-by-build-name/help-regex-job-filter.html";
        }
    }
}