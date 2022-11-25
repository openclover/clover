for plugin in org.eclipse.ant.core \
  org.eclipse.core.commands \
  org.eclipse.core.contenttype \
  org.eclipse.core.jobs \
  org.eclipse.core.resources \
  org.eclipse.core.runtime \
  org.eclipse.debug.core \
  org.eclipse.debug.ui \
  org.eclipse.e4.ui.workbench \
  org.eclipse.equinox.common \
  org.eclipse.equinox.preferences \
  org.eclipse.equinox.registry \
  org.eclipse.jdt.core \
  org.eclipse.jdt.debug.ui \
  org.eclipse.jdt.junit.core \
  org.eclipse.jdt.junit \
  org.eclipse.jdt.launching \
  org.eclipse.jdt.ui \
  org.eclipse.jface.text \
  org.eclipse.jface \
  org.eclipse.osgi \
  org.eclipse.swt.win32.win32.x86_64 \
  org.eclipse.text \
  org.eclipse.ui.editors \
  org.eclipse.ui.forms \
  org.eclipse.ui.ide \
  org.eclipse.ui.workbench.texteditor \
  org.eclipse.ui.workbench; do

version=`ls target/extract | grep "${plugin}_" | sed "s/${plugin}_//" | sed "s/\.jar//"`
echo "<${plugin}.version>${version}</${plugin}.version>"

done